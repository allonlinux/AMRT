package fr.free.allonlinux.amrt.media.video;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.free.allonlinux.amrt.AMRT;
import fr.free.allonlinux.amrt.media.MediaCodecEnum;
import fr.free.allonlinux.amrt.media.MediaDescriptor;
import fr.free.allonlinux.amrt.media.MediaItem;
import fr.free.allonlinux.amrt.media.MediaTypeEnum;
import fr.free.allonlinux.amrt.utils.ByteSequence;

public class VideoDecoder
{
	// --> frame detection : 00 00 00 02 09
	static private final byte VideoStreamPattern[]={0x00,0x00,0x00,0x02,0x09};
	// --> audio detection : 21
	static private final byte AudioStreamPattern[]={0x21};

	/**
	 * Read the information from the header :
	 * - header size at position '0x20'
	 * - video size at position '0x270' (width) and '0x274' (height)
	 * - data size at position 'header_size-4'
	 * - offsets of video and audio chunks in 'stco' atom
	 * - creation date and duration in 'mvhd' atom
	 */
	public static Video readHeader(FileChannel  i__stream, long i__headerOffset, MediaDescriptor i__pattern) throws IOException
	{
		// Create new Video
		Video l__result=new Video();
		
		// Set codec type
		l__result.codec=i__pattern.codec;
		
		// Temp data
		String l__string="";
		ByteBuffer l__4Bytes = ByteBuffer.allocate(4);
		ByteBuffer l__2Bytes = ByteBuffer.allocate(2);
		//ByteBuffer l__1Byte = ByteBuffer.allocate(1);l__1Byte.clear();
		
		// Get header size : 
		// - 4 bytes at 'i__headerOffset+0x20' position
		i__stream.position(i__headerOffset+0x20);
		l__4Bytes.clear();i__stream.read(l__4Bytes);l__4Bytes.rewind();
		l__result.headerSize=l__4Bytes.getInt()+0x20; // header = size + pre-header=0x20
		// - 4 bytes to get the "free" ATOM size - if it exists
		if ( i__pattern.codec.equals(MediaCodecEnum.MP4_avc1) )
		{
			i__stream.position(i__headerOffset+l__result.headerSize+4);
			l__4Bytes.clear();i__stream.read(l__4Bytes);l__4Bytes.rewind();
			l__string=new String(l__4Bytes.array(), "ASCII");
			if ( l__string.equals("free") )
			{
				i__stream.position(i__headerOffset+l__result.headerSize);
				l__4Bytes.clear();i__stream.read(l__4Bytes);l__4Bytes.rewind();
				l__result.headerSize+=l__4Bytes.getInt();
			}
		}
		// - 8 bytes
		l__result.headerSize+=8;
		// Header size is modulo 'FILESYSTEM_CLUSTER_SIZE'
		if ( (l__result.headerSize % AMRT.g__filesystemClusterSize) != 0)
		{
			AMRT.g__log.log(Level.WARNING,"The header size should be a factor of the cluster size : "+l__result.headerSize);
			l__result.headerSize=((l__result.headerSize / AMRT.g__filesystemClusterSize)+1) * AMRT.g__filesystemClusterSize;
		}
		
		// Get the video size : 
		// -> 2 bytes at position 0x0270 (width) and 0x0274 (height)
		i__stream.position(i__headerOffset+0x270);
		l__2Bytes.clear();i__stream.read(l__2Bytes);l__2Bytes.rewind();
		l__result.videoWidth=l__2Bytes.getShort();
		i__stream.position(i__headerOffset+0x274);l__2Bytes.rewind();
		l__2Bytes.clear();i__stream.read(l__2Bytes);l__2Bytes.rewind();
		l__result.videoHeight=l__2Bytes.getShort();
		
		// Check that the header is valid by ckecking if the last 4 bytes are 'mdat'
		i__stream.position(i__headerOffset+l__result.headerSize-4);
		l__4Bytes.clear();i__stream.read(l__4Bytes);l__4Bytes.rewind();
		l__string=new String(l__4Bytes.array(), "ASCII");
		if ( !l__string.equals("mdat") )
		{
			throw new IOException("Failed to read the header : not valid");
		}
		
		// Get the dataSize
		i__stream.position(i__headerOffset+l__result.headerSize-8);
		l__4Bytes.clear();i__stream.read(l__4Bytes);l__4Bytes.rewind();
		l__result.dataSize=l__4Bytes.getInt();
		// - in case of a video bigger than Integer.MAX_VALUE (>2GB)
		if (l__result.dataSize<0)
		{
			l__result.dataSize=Integer.MAX_VALUE-l__result.dataSize; 
		}
		// - modulo
		if ( (l__result.dataSize % AMRT.g__filesystemClusterSize) != 0)
		{
			l__result.dataSize=((l__result.dataSize / AMRT.g__filesystemClusterSize)+1) * AMRT.g__filesystemClusterSize;
		}
		
		// Read Chunk Offset Atoms
		// 1/ Load the header in memory
		byte[] l__bufferArray = new byte[(int)l__result.headerSize];
		ByteBuffer l__headerBytes = ByteBuffer.wrap(l__bufferArray);
		i__stream.position(i__headerOffset);
		i__stream.read(l__headerBytes);
		
		// 2/ Create Matcher to find the occurence of 'stco' in the header
		// stco + 4 bytes at 0x00 + 4 bytes (nb_frames/audio) + 4*nb_frames/audio offset
		Pattern l__offsetPattern = Pattern.compile("stco");
		Matcher l__matcher = l__offsetPattern.matcher(new ByteSequence(l__bufferArray));
		
		// - 1st occurence (video)
		if (l__matcher.find() )
		{
			int l_stcoOffset=l__matcher.start();
			//AMRT.LOG.log(Level.INFO,"Offset :- "+l_stcoOffset);
			l__headerBytes.position(l_stcoOffset+8);
			int l__nbVideoChunks=l__headerBytes.getInt();
			for(int i=0;i<l__nbVideoChunks;i++)
			{
				int l__currentSize=l__headerBytes.getInt();
				l__result.offsets.add(new MediaItem(l__currentSize, MediaTypeEnum.Video));
				//System.out.printf("Offset : %x\n",l__currentSize);
			}
			// Set the number of frames
			l__result.nbFrames=l__nbVideoChunks;
		}
		
		// - 2nd occurence (audio)
		/*if (l__matcher.find() ) {
			int l_stcoOffset=l__matcher.start();
			//AMRT.LOG.log(Level.INFO,"Offset :- "+l_stcoOffset);
			l__headerBytes.position(l_stcoOffset+8);
			int l__nbAudioChunks=l__headerBytes.getInt();
			for(int i=0;i<l__nbAudioChunks;i++) {
				int l__currentSize=l__headerBytes.getInt();
				l__result.offsets.add(new MediaOccurence(MediaType.Audio,l__currentSize));
			}
		}*/
		//AMRT.LOG.log(Level.INFO,l__nbVideoChunks+" - "+l__nbAudioChunks);
		
		// - sort offsets
		Collections.sort(l__result.offsets, new Comparator<MediaItem>(){
			public int compare(MediaItem l__val1, MediaItem l__val2) {
				return (int)(l__val1.offset - l__val2.offset);
			}
		});

		// Read Movie Header Atom
		Pattern l__creationTimePattern = Pattern.compile("mvhd");
		l__matcher = l__creationTimePattern.matcher(new ByteSequence(l__bufferArray));
		if ( l__matcher.find() )
		{
			int l__mvhdOffset=l__matcher.start();
			
			// - get creation time
			l__headerBytes.position(l__mvhdOffset+4);
			l__result.creationTime=new Date((l__headerBytes.getLong() - 2082844800L) * 1000L);
			
			// - get duration
			l__headerBytes.position(l__mvhdOffset+16);
			int l__timeScale=l__headerBytes.getInt();
			int l__duration=l__headerBytes.getInt();
			l__result.duration=l__duration/l__timeScale;
		}
			
		return l__result;
	}

	public static void recover(FileChannel  i__stream, MediaItem i__video)
	{
		AMRT.g__log.log(Level.INFO,"\n\n##### Recover video with Protune 'ON' (MP4) #####");
		AMRT.g__log.log(Level.INFO,"MP4 @ 0x%x\n",i__video.offset);
		
		FileOutputStream l__outputFileStreamMP4=null;
		Video l__video=null;
		
		try
		{
			// Read the video information from the video
			l__video=VideoDecoder.readHeader(i__stream, i__video.offset,i__video.descriptor);
			AMRT.g__log.log(Level.INFO,l__video.toString());
			
			// Create the output file
			l__outputFileStreamMP4=new FileOutputStream(AMRT.g__outputDirectory+"GOPRO__"+l__video.getCreationTime()+".MP4");
			FileChannel l__outputMP4=l__outputFileStreamMP4.getChannel();
			
			// Copy the header
			i__stream.transferTo( i__video.offset,l__video.headerSize, l__outputMP4);
			
			// Calculate the estimate position of the video data
			// Note : we decrease the offset by "l__video.headerSize" because the offset of the block is relative to the absolute file (=with the header)
			long l__firstOffset=i__video.offset-l__video.dataSize-l__video.headerSize;
			
			// It seems that, sometimes, there is a small gap of empty clusters between the data and the header.
			// Because of that, we must go backward a little bit to be sure to get the beginning of the data...
			l__firstOffset=Math.max(AMRT.g__lastHeaderOffset, l__firstOffset - 20*AMRT.g__filesystemClusterSize);
			
			// Find the beginning of the MP4 video
			while ( true )
			{
				// Compare
				if ( isMediaPresent(i__stream, l__firstOffset+l__video.offsets.get(0).offset, MediaTypeEnum.Video) )
				{
					break;
				} 
				else
				{
					l__firstOffset+=AMRT.g__filesystemClusterSize;
				}
				
				// Check we are not going too far...
				if ( l__firstOffset>= i__video.offset )
				{
					AMRT.g__log.log(Level.SEVERE,"Could not find the first offset of the video");
					return;
				}		
			}
			AMRT.g__log.log(Level.FINE,"First offset of the video found at position : 0x%x",l__firstOffset);
			
			VideoItem l__videoPlop=new VideoItem(l__video,l__outputMP4);
			
			// Write the header and data
			ArrayList<MediaItem> l__offsets=l__video.offsets;
			MediaItem l__previous=null;
			for(Iterator<MediaItem> it=l__offsets.iterator();it.hasNext();)
			{
				MediaItem l__current=it.next();
				
				// Check that the pattern is at the good place
				if ( ! VideoDecoder.isMediaPresent(i__stream,l__firstOffset+l__current.offset,l__current.descriptor.type) )
				{
					AMRT.g__log.log(Level.SEVERE,"Couldn't find the pattern !");
					return;
				}
				
				// Depending on the camera, there are sometimes data between the end of the header and the very first chunk...
				if ( l__previous == null )
				{
					AMRT.g__log.log(Level.INFO,"Copy preliminary data = "+(l__current.offset%AMRT.g__filesystemClusterSize)+" at "+(l__firstOffset+l__video.headerSize));
					i__stream.transferTo(l__firstOffset+l__video.headerSize, l__current.offset%AMRT.g__filesystemClusterSize, l__videoPlop.channel);
				}
				// Copy the previous block
				else
				{
					i__stream.transferTo(l__firstOffset+l__previous.offset, l__current.offset-l__previous.offset, l__videoPlop.channel);
				}
				
				l__previous=l__current;
			}
			
			AMRT.g__log.log(Level.FINE,"Output file size = "+l__outputMP4.size());
			AMRT.g__log.log(Level.FINE,"Header offset = "+i__video.offset);
			AMRT.g__log.log(Level.FINE,"Virtual header offset = "+l__firstOffset);
			AMRT.g__log.log(Level.FINE,"First data offset = "+l__video.offsets.get(0).offset);
			AMRT.g__log.log(Level.FINE,"Last data offset = "+l__video.offsets.get(l__video.offsets.size()-1).offset);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			// Close output file streams
			try
			{
				if ( l__outputFileStreamMP4 != null )
				{
					// Print report
					long l__percentage=(100*l__outputFileStreamMP4.getChannel().size())/(l__video.headerSize+l__video.dataSize);
					if (l__percentage > 100)
						l__percentage=100;
					if (l__percentage < 0)
						l__percentage=0;
					AMRT.g__log.log(Level.INFO,"Recovery percentage : "+l__percentage);
					
					l__outputFileStreamMP4.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	static byte g__comparisonArray[]=new byte[Math.max(16,Math.max(VideoStreamPattern.length, AudioStreamPattern.length))];
	static ByteBuffer g__comparisonBuffer = ByteBuffer.wrap(g__comparisonArray);
	
	public static boolean isMediaPresent(FileChannel  i__stream, long i__offset, MediaTypeEnum i__type) throws IOException
	{
		// Read buffer
		g__comparisonBuffer.clear();
		i__stream.position(i__offset);
		i__stream.read(g__comparisonBuffer);
		
		if ( i__type == MediaTypeEnum.Video )
		{				
			// Compare
			if ( g__comparisonArray[0] == VideoStreamPattern[0] && g__comparisonArray[1] == VideoStreamPattern[1] && g__comparisonArray[2] == VideoStreamPattern[2] && g__comparisonArray[3] == VideoStreamPattern[3] && g__comparisonArray[4] == VideoStreamPattern[4] )
			{
				return true;
			}
		}
		else
		{
			// Compare
			if ( g__comparisonArray[0] == AudioStreamPattern[0] )
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean recover(FileChannel  i__stream, MediaItem i__mediaTHM, MediaItem i__mediaMP4, MediaItem i__mediaLRV)
	{
		AMRT.g__log.log(Level.INFO,"\n##### Recover video with Protune 'OFF' (THM/MP4/LRV) #####\n");
		if ( i__mediaTHM != null )
			AMRT.g__log.log(Level.INFO,"THM @ 0x%x",i__mediaTHM.offset);
		if ( i__mediaMP4 != null )
			AMRT.g__log.log(Level.INFO,"MP4 @ 0x%x",i__mediaMP4.offset);
		if ( i__mediaLRV != null )
			AMRT.g__log.log(Level.INFO,"LRV @ 0x%x",i__mediaLRV.offset);
		AMRT.g__log.log(Level.INFO,"\n");
		
		FileOutputStream l__outputFileStreamMP4=null;
		FileOutputStream l__outputFileStreamLRV=null;
		
		Video l__videoMP4=null;
		Video l__videoLRV=null;
		
		
		try
		{
			// Read the video information from both videos
			l__videoMP4=VideoDecoder.readHeader(i__stream, i__mediaMP4.offset,i__mediaMP4.descriptor);
			l__videoLRV=VideoDecoder.readHeader(i__stream, i__mediaLRV.offset,i__mediaLRV.descriptor);
			// Check that we didn't mismatch the LRV and MP4 files
			// If so, switch them...
			if ( l__videoLRV.dataSize > l__videoMP4.dataSize )
			{
				Video l__tmpVideo=l__videoMP4;
				l__videoMP4=l__videoLRV;
				l__videoLRV=l__tmpVideo;
				
				MediaItem l__tmpItem=i__mediaMP4;
				i__mediaMP4=i__mediaLRV;
				i__mediaLRV=l__tmpItem;
			}
			
			// Check that we are processing the good videos
			// Note : there can be 1-2 seconds of difference between the creation timestamp of the MP4 and LRV files...
			if ( Math.abs(l__videoMP4.creationTime.getTime()-l__videoLRV.creationTime.getTime()) > 2000 )
			{
				AMRT.g__log.log(Level.WARNING,"The creation time of the MP4 and LRV videos doesn't match... Next ! "+l__videoMP4.getCreationTime()+" - "+l__videoLRV.getCreationTime());
				return false;
			}
			// Check that the THM header  is close to the MP4 and LRV headers
			/*if ( i__mediaTHM !=null )
			{
				if ( Math.abs(i__mediaTHM.offset-i__mediaMP4.offset) > l__videoMP4.dataSize/2 && Math.abs(i__mediaTHM.offset-i__mediaLRV.offset) > l__videoLRV.dataSize/2 )
				{
					AMRT.g__log.log(Level.WARNING,"The TLV position compared to the MP4 and LRV videos doesn't match... Next ! "+i__mediaTHM.offset+" - "+i__mediaMP4.offset+" - "+i__mediaLRV.offset);
					return false;
				}
			}*/
			
			// Now that we know we are processing the good videos, print their information
			AMRT.g__log.log(Level.INFO,l__videoMP4.toString());
			AMRT.g__log.log(Level.INFO,l__videoLRV.toString());
			
			// Get the offset of the MP4 video
			long l__firstMP4Offset=0;
			l__firstMP4Offset=(i__mediaMP4.offset-l__videoLRV.dataSize-l__videoMP4.dataSize)+2*AMRT.g__filesystemClusterSize+(l__videoMP4.offsets.get(0).offset %AMRT.g__filesystemClusterSize);
			
			if ( l__firstMP4Offset < 0 )
			{
				l__firstMP4Offset=0;
			}
			
			// Check that the offset is valid (= not before the last detected video)
			if ( l__firstMP4Offset < AMRT.g__lastHeaderOffset )
			{
				AMRT.g__log.log(Level.WARNING,"The offset of the video is not valid : "+l__firstMP4Offset +" < "+ AMRT.g__lastHeaderOffset);
			}
			
			// It seems that, sometimes, there is a small gap of empty clusters between the data and the header.
			// Because of that, we must go backward a little bit to be sure to get the beginning of the data...
			l__firstMP4Offset=Math.min(l__firstMP4Offset, Math.max(AMRT.g__lastHeaderOffset+(l__videoMP4.offsets.get(0).offset %AMRT.g__filesystemClusterSize), l__firstMP4Offset - 20*AMRT.g__filesystemClusterSize));
			
			// ... and then, go forward again in order to find the beginning of the MP4 video
			while ( true )
			{
				// Compare
				if ( isMediaPresent(i__stream, l__firstMP4Offset, MediaTypeEnum.Video) )
				{
					break;
				}
				else
				{
					l__firstMP4Offset+=AMRT.g__filesystemClusterSize;
				}
				
				// Check we are not going too far...
				if ( l__firstMP4Offset>= i__mediaMP4.offset )
				{
					AMRT.g__log.log(Level.SEVERE,"Could not find the first offset of the video");
					return false;
				}		
			}
			AMRT.g__log.log(Level.INFO,"First offset of the video found at position : 0x%x",l__firstMP4Offset);
			
	
			// Create output files
			FileChannel l__outputTHM=null;
			if (i__mediaTHM !=null )
				l__outputTHM=(new FileOutputStream(AMRT.g__outputDirectory+"GOPRO_"+l__videoMP4.getCreationTime()+".THM")).getChannel();
			FileChannel l__outputMP4=(l__outputFileStreamMP4=new FileOutputStream(AMRT.g__outputDirectory+"GOPRO_"+l__videoMP4.getCreationTime()+".MP4")).getChannel();
			FileChannel l__outputLRV=(l__outputFileStreamLRV=new FileOutputStream(AMRT.g__outputDirectory+"GOPRO_"+l__videoMP4.getCreationTime()+".LRV")).getChannel();
			// In debug mode, we can use /dev/null as the output file if we don't want to write the videos (fasten the debugging)
			//FileChannel l__outputMP4=(l__outputFileStreamMP4=new FileOutputStream("/dev/null")).getChannel();
			//FileChannel l__outputLRV=(l__outputFileStreamLRV=new FileOutputStream("/dev/null")).getChannel();
	
			VideoItem l__current=new VideoItem(l__videoMP4,l__outputMP4);
			l__current.blockOffset=l__firstMP4Offset-(l__videoMP4.offsets.get(0).offset %AMRT.g__filesystemClusterSize);
			l__current.blockSize=l__current.video.offsets.get(1).offset-l__current.video.offsets.get(0).offset+(l__videoMP4.offsets.get(0).offset %AMRT.g__filesystemClusterSize);
			
			VideoItem l__other=new VideoItem(l__videoLRV,l__outputLRV);
			l__other.blockOffset=0;
			l__other.blockSize=0;
			l__other.blockIndex=-1;
			
			// Write THM file
			if (i__mediaTHM !=null )
			{
				i__stream.transferTo( i__mediaTHM.offset,AMRT.g__filesystemClusterSize, l__outputTHM);
				l__outputTHM.close();
			}
			
			// Write MP4 and LRV headers
			i__stream.transferTo( i__mediaMP4.offset,l__videoMP4.headerSize, l__outputMP4);
			i__stream.transferTo( i__mediaLRV.offset, l__videoLRV.headerSize, l__outputLRV);
			
			boolean l__skipNext=false;
			
			while ( l__current != null )
			{
				// Check if next block is in the middle of the THM, as it seems that it can be in the middle of the video data...
				if (i__mediaTHM !=null)
				{
					if ( (l__current.blockOffset <  i__mediaTHM.offset) && (l__current.blockOffset+l__current.blockSize > i__mediaTHM.offset) )
					{
						AMRT.g__log.log(Level.INFO,"Skipping cluster because the current one belongs to the THM media");
						l__current.blockOffset+=AMRT.g__filesystemClusterSize;
						l__skipNext=true;
					}
				}
				
				// Check if next block is present at the good position
				boolean l__isPresent=isMediaPresent(i__stream,l__current.blockOffset+l__current.blockSize,l__current.video.offsets.get(l__current.blockIndex+1).descriptor.type);
				
				// Check that we are reading the good video <-- in case of an offset overlapping between the two videos...
				if (l__isPresent)
					l__isPresent=l__current.sanityCheck(g__comparisonArray);
				
				// If next block is found
				if ( l__isPresent )
				{
					AMRT.g__log.log(Level.FINEST,"Offset of the next block stream found at position : 0x%x",l__current.blockOffset+l__current.blockSize);
					
					// Copy to the output file
					try
					{
						// In case next block is in the middle of the THM
						if ( l__skipNext )
						{
							i__stream.transferTo(l__current.blockOffset-AMRT.g__filesystemClusterSize, l__current.blockSize, l__current.channel);
							l__skipNext=false;
						}
						// In normal case
						else
						{
							i__stream.transferTo( l__current.blockOffset, l__current.blockSize, l__current.channel);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
					// Check if it was the last block
					if ( l__current.blockIndex+2 >= l__current.video.offsets.size() )
					{
						if ( l__other == null )
						{
							AMRT.g__log.log(Level.INFO,"The END !!!");
							break; // <--- THE END
						}
						else
						{
							l__current=l__other;
							l__other=null;
							continue;
						}
					}
					
					// Update next block
					l__current.blockIndex++;
					l__current.blockOffset+=l__current.blockSize;
					l__current.blockSize=l__current.video.offsets.get(l__current.blockIndex+1).offset-l__current.video.offsets.get(l__current.blockIndex).offset;
					if (l__current.blockSize <= 0)
					{
						AMRT.g__log.log(Level.SEVERE,"Bad block size");
						return true;
					}
				} 
				// The next block of the "current" video  has not been found. We now need to :
				// - find where starts the next cluster of the "other" video
				// - write the potential remaining clusters from the "current" video (which don't contain the next block pattern)
				// - update the information about the next block for both videos
				// - switch "current" and "other" for the next iteration of the loop
				else
				{
					// Check if the "other" video is already completely processed
					if ( l__other == null )
					{
						break;
					}
					
					// Find the next occurence of the "other" video from the beginning of the next cluster
					// - calculate the number of bytes to the end of the cluster, from the last block of the "current" video
					long l__modulo=AMRT.g__filesystemClusterSize-(l__current.blockOffset% AMRT.g__filesystemClusterSize);
					// - the starting offset to find the "other video" block is the beginning of the next cluster 
					long l__startOffset=l__current.blockOffset+l__modulo;
					long l__currentOffset=l__startOffset;
					
					// If it is the first offset of the LRV video
					if ( l__other.blockIndex < 0 )
					{
						l__other.blockSize=l__videoLRV.offsets.get(0).offset %AMRT.g__filesystemClusterSize;
					}
					
					// Search for the next block of the "other" video in the next clusters
					while ( true )
					{
						// Check that we are not in the middle of the THM, as it seems that it can be in the middle of the video data...
						if (i__mediaTHM !=null)
						{
							if ( (l__current.blockOffset <  i__mediaTHM.offset) && (l__current.blockOffset+l__current.blockSize > i__mediaTHM.offset) )
							{
								AMRT.g__log.log(Level.INFO,"Skipping cluster because the current one belongs to the THM media");
								l__current.blockOffset+=AMRT.g__filesystemClusterSize;
							}
						}
						
						// Check if the block of the "other" video is present in the current cluster
						if ( isMediaPresent(i__stream, l__currentOffset+l__other.blockSize, l__other.video.offsets.get(l__other.blockIndex+1).descriptor.type) )
						{
							break;
						}
						// Otherwise, continue with the next cluster
						else
						{
							l__currentOffset+=AMRT.g__filesystemClusterSize;
							
							// But before, check that we are not going too far...
							/*if ( l__currentOffset > i__mediaMP4.offset )
							{
								AMRT.g__log.log(Level.SEVERE,"We are going too far... stop !");
								return true;
							}*/
						}	
					}
					
					// Copy the remaining bytes of the last cluster from the "current" video
					i__stream.transferTo( l__current.blockOffset, l__currentOffset-l__current.blockOffset, l__current.channel);
	
					// Sanity check : 
					// - if the next "block" is supposed to be in the current "cluster", there is a big problem because we didn't find it...
					if (l__current.blockSize <= l__currentOffset-l__current.blockOffset)
					{
						// - but, unfortunately, the next block pattern of the "current" video can be split between two clusters : starts at the end of cluster 'n' and ends at the beginning of cluster 'n+i'
						// with 'i' clusters of the "other" video in the  middle...
						long l__remainingSize= ( l__currentOffset-l__current.blockOffset)-l__current.blockSize ;
						if ( l__current.video.offsets.get(l__current.blockIndex+1).descriptor.type == MediaTypeEnum.Video && l__remainingSize < VideoStreamPattern.length ||
								l__current.video.offsets.get(l__current.blockIndex+1).descriptor.type == MediaTypeEnum.Audio && l__remainingSize < AudioStreamPattern.length  )
						{
							AMRT.g__log.log(Level.INFO,"Next block pattern on two clusters separated by the other video");
							
							// Increment the 'blockIndex' because we found the next block !
							l__current.blockIndex++;
							// Update the block information of the "current" video and set the 'blockOffset' to '0'.
							// It will be updated with the good value the next time we switch the videos
							l__current.blockSize=l__current.video.offsets.get(l__current.blockIndex+1).offset-l__current.video.offsets.get(l__current.blockIndex).offset-l__remainingSize;
							l__current.blockOffset=0;
						}
						else
						{
							AMRT.g__log.log(Level.SEVERE,"Bad block size : "+l__current.blockSize+"/"+l__currentOffset+"/"+l__current.blockOffset);
							return true;
						}
					}
					else
					{
						// Update the block information of the "current" video and set the 'blockOffset' to '0'.
						// It will be updated with the good value the next time we switch the videos
						l__current.blockSize -=  l__currentOffset-l__current.blockOffset;
						l__current.blockOffset = 0;
					}
					
					// Update the block information of the "other" video
					l__other.blockOffset=l__currentOffset;
					if ( l__other.blockIndex == 0 )
					{
						l__other.blockSize = l__other.video.offsets.get(1).offset-l__other.video.offsets.get(0).offset;
						if (l__other.blockSize <= 0)
						{
							AMRT.g__log.log(Level.SEVERE,"Bad block size : "+l__other.blockSize);
							return true;
						}
						l__other.blockIndex=0;
					}
					
					// Switch videos, so we continue processing the "other" video
					VideoItem l__tmp=l__current;
					l__current=l__other;
					l__other=l__tmp;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			// Close output file streams
			try
			{
				if ( l__outputFileStreamMP4 != null )
				{	
					// Print report
					long l__percentage=(100*l__outputFileStreamMP4.getChannel().size())/(l__videoMP4.headerSize+l__videoMP4.dataSize);
					if (l__percentage > 100)
						l__percentage=100;
					if (l__percentage < 0)
						l__percentage=0;
					AMRT.g__log.log(Level.INFO,"Recovery percentage : "+l__percentage);
					
					l__outputFileStreamMP4.close();
				}
				if ( l__outputFileStreamLRV != null )
					l__outputFileStreamLRV.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return true;
	}
}
