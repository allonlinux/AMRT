package fr.free.allonlinux.amrt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaFinder {
	
	/** Size within which we will look for the pattern	in the block read form disk */
	private static int MAX_HEADER_FIND_SIZE=30;
	
	/**
	 * Find a pattern in a subregion of the input buffer.
	 * 
	 * @param i__pattern
	 * @param i__buffer
	 * @param i__start
	 * @param i__end
	 * @return
	 */
	static int findPattern(Pattern i__pattern, byte i__buffer[], int i__start, int i__end) {
		// Create Matcher
		Matcher l__matcher = i__pattern.matcher(new ByteSequence(i__buffer));
		// Set search region
		l__matcher.region(i__start, i__end);
		// Search
		if ( l__matcher.find() ) {
			return l__matcher.start();
		}
		return -1;
	}
	
	/**
	 * Find a pattern in the input buffer.
	 * 
	 * @param i__pattern
	 * @param i__buffer
	 * @return
	 */
	static int findPattern(Pattern i__pattern, byte i__buffer[]) {
		return findPattern(i__pattern, i__buffer, 0, i__buffer.length);
	}
	
	/**
	 * Find medias in a subregion of the input file.
	 * 
	 * @param i__stream
	 * @param i__start
	 * @param i__end
	 * @throws IOException
	 */
	static ArrayList<MediaOccurence> findMedia(FileChannel  i__stream, long i__start, long i__end) throws IOException {
		// Create the output list
		ArrayList<MediaOccurence> l__result=new ArrayList<MediaOccurence>();
		
		
		// Buffer used to store the current block of the file
		byte[] array = new byte[AMRT.FILESYSTEM_CLUSTER_SIZE];
		ByteBuffer l__buffer = ByteBuffer.wrap(array);
		
		// Move to start offset
		long l__currentOffset=i__start;
		i__stream.position(l__currentOffset);
		
		// Read the file block by block
		while( i__stream.read(l__buffer) > 0 )
		{
			// For each block, try to find a media header
			for ( int i=0;i<AMRT.headers.length;i++) {
				// Current media to find
				MediaPattern l__media=AMRT.headers[i];
				// Find the media, thanks to its pattern
				// Only the first MAX_HEADER_FIND_SIZE bytes are really used
				int l__offset=findPattern(l__media.pattern,array,0,MAX_HEADER_FIND_SIZE);
				// If a media is found...
				if (l__offset >= 0 ) {
					AMRT.LOG.log(Level.INFO,"Media found : '%s - %s' at position 0x%x\n",new Object[]{l__media.type, l__media.codec,(l__currentOffset+l__offset)});
					// ... add it to the output list
					l__result.add(new MediaOccurence(l__currentOffset, l__media));
				}
			}
			
			// Clear buffer
			l__buffer.clear();
			
			// Move to the next cluster
			l__currentOffset+=AMRT.FILESYSTEM_CLUSTER_SIZE;
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
	public static ArrayList<MediaOccurence> find(FileChannel  i__stream) throws IOException {
		return findMedia(i__stream,0,i__stream.size());
	}
}
