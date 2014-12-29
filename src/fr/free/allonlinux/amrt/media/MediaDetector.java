package fr.free.allonlinux.amrt.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.free.allonlinux.amrt.AMRT;
import fr.free.allonlinux.amrt.gui.DetectionPanel;
import fr.free.allonlinux.amrt.utils.ByteSequence;

public class MediaDetector
{
	/** Size in bytes within which we will look for the pattern	in the block read from the file */
	private static final int MAX_HEADER_FIND_SIZE=30;
	
	/**
	 * Find a pattern in a subregion of the input buffer.
	 * 
	 * @param i__pattern
	 * @param i__buffer
	 * @param i__start
	 * @param i__end
	 * @return
	 */
	static private int findPattern(Pattern i__pattern, byte i__buffer[], int i__start, int i__end)
	{
		// Create Matcher
		Matcher l__matcher = i__pattern.matcher(new ByteSequence(i__buffer));
		// Set search region
		l__matcher.region(i__start, i__end);
		// Search
		if ( l__matcher.find() )
		{
			return l__matcher.start();
		}
		return -1;
	}
	
	
	/**
	 * Find medias in a subregion (i__start -> i__end) of the input file, by iterating on each block of the file
	 * and trying to find a known pattern at the beginning of the block.
	 * 
	 * @param i__stream
	 * @param i__start
	 * @param i__end
	 * @throws IOException
	 */
	static private ArrayList<MediaItem> findMedia(FileChannel  i__stream, long i__start, long i__end) throws IOException
	{
		// Create the output list
		ArrayList<MediaItem> l__result=new ArrayList<MediaItem>();
		
		// Buffer used to store the current block of the file
		byte[] array = new byte[AMRT.g__filesystemClusterSize];
		ByteBuffer l__buffer = ByteBuffer.wrap(array);
		
		// Move to start offset
		long l__currentOffset=i__start;
		i__stream.position(l__currentOffset);
		
		// Read the file block by block
		while( i__stream.read(l__buffer) > 0 )
		{
			// For each block, try to find a media header
			for ( int i=0;i<AMRT.g__headers.length;i++) 
			{
				// Current media to find
				MediaDescriptor l__media=AMRT.g__headers[i];
				// Find the media, thanks to its pattern
				// Only the first MAX_HEADER_FIND_SIZE bytes are really used
				int l__offset=findPattern(l__media.pattern,array,0,MAX_HEADER_FIND_SIZE);
				// If a media is found...
				if (l__offset >= 0 )
				{
					AMRT.g__log.log(Level.INFO,"Media found : '%s - %s' at position 0x%x",new Object[]{l__media.type, l__media.codec,(l__currentOffset+l__offset)});
					
					MediaItem l__item = new MediaItem(l__currentOffset, l__media);
					// ... add it to the output list
					l__result.add(l__item);
					
					if ( DetectionPanel.g__tableModel != null )
					{
						Vector<Object> vector = new Vector<Object>();
						vector.add(l__item.descriptor.type.toString());
						vector.add(l__item.descriptor.codec.toString());
						vector.add("0x"+Long.toHexString(l__item.offset));
						
						DetectionPanel.g__tableModel.insertData(vector);
					}
				}
			}
			
			// Clear buffer
			l__buffer.clear();
			
			// Move to the next cluster
			l__currentOffset+=AMRT.g__filesystemClusterSize;
			i__stream.position(l__currentOffset);
			
			if ( l__currentOffset  >= i__end )
				break;
		}
		
		return l__result;
	}
	
	
	/**
	 * Find medias in the whole content of the input file
	 * 
	 * @param i__stream
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<MediaItem> findMedia(FileChannel  i__stream) throws IOException
	{
		return findMedia(i__stream,0,i__stream.size());
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<MediaItem> detect()
	{
		RandomAccessFile l__file=null;
		ArrayList<MediaItem> l__mediaList=null;
		
		try
		{
			// Open file
			l__file = new RandomAccessFile(AMRT.g__inputFile, "r");
			FileChannel l__channel=l__file.getChannel();
			
			// Search for medias
			File l__mediaListFile=new File(AMRT.g__outputDirectory+"/"+"tmp_offsets.ser");
			// - check for previously found medias
			if ( !AMRT.g__skipPreviousDetection && l__mediaListFile.exists() )
			{
				FileInputStream l__fileInputStrean = new FileInputStream(l__mediaListFile);
				ObjectInputStream l__objectInputStream = new ObjectInputStream(l__fileInputStrean);
				l__mediaList = (ArrayList<MediaItem>) l__objectInputStream.readObject();
				l__objectInputStream.close();
				l__fileInputStrean.close();
				
				if ( DetectionPanel.g__tableModel != null )
				{
					for(Iterator<MediaItem> i=l__mediaList.iterator(); i.hasNext();)
					{
						MediaItem current=i.next();
						
						Vector<Object> vector = new Vector<Object>();
						vector.add(current.descriptor.type.toString());
						vector.add(current.descriptor.codec.toString());
						vector.add("0x"+Long.toHexString(current.offset));
						
						DetectionPanel.g__tableModel.insertData(vector);
					}
				}
			}
			// - otherwise, find them and serialize the result
			else
			{
				// find
				l__mediaList=MediaDetector.findMedia(l__channel);
				// serialize
				FileOutputStream l__fileOutputStream = new FileOutputStream(l__mediaListFile);
				ObjectOutputStream l__objectOutputStream = new ObjectOutputStream(l__fileOutputStream);
				l__objectOutputStream.writeObject(l__mediaList);
				l__objectOutputStream.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			// Close file
			if (l__file != null )
			{
				try
				{
					l__file.close();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		}
		
		return l__mediaList;
	}
}
