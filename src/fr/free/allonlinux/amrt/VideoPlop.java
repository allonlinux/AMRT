package fr.free.allonlinux.amrt;

import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

public class VideoPlop {

	Video video;
	FileChannel channel;
	
	int blockIndex;						/** Current block index */
	long blockOffset; 					/** Absolute offset in the file */
	long blockSize;						/** Block size */
	

	static final int MAX_SIZE_BLOCK_HEADERS=6;
	LinkedList<byte[]> previousBlockHeaders=new LinkedList<byte[]>();	/** Circular list with the last MAX_SIZE_BLOCK_HEADERS block headers  */
	
	public VideoPlop(Video i__video, FileChannel i__channel) {
		video=i__video;
		channel=i__channel;
		blockIndex=0;
	}
	
	
	/**
	 * Check that the newly detected buffer that is supposed to be part of the video is
	 * not from the other video.
	 * 
	 * --> According to my experimentations, the first 16 bytes of the frame block are 
	 * always common to the MP4 and LRV videos. And they are never the same for 6
	 * successive frames... Let's cross fingers that it is always true...
	 * 
	 * @param i__newBuffer
	 * @return
	 */
	public boolean validityCheck(byte i__newBuffer[]) {
		// Check if the input buffer is not already present
		for(Iterator<byte[]> it=previousBlockHeaders.iterator();it.hasNext();) {
			byte [] l__current=it.next();
			
			boolean l__found=true;
			for(int i=0;i<Math.min(l__current.length,i__newBuffer.length);i++) {
				if (l__current[i] != i__newBuffer[i]) {
					l__found=false;
					break;
				}
			}
			
			if (l__found) {
				AMRT.LOG.log(Level.WARNING,"Prevented an offset overlapping");
				return false;
			}
		}
		
		// Remove the eldest
		if (previousBlockHeaders.size() >= MAX_SIZE_BLOCK_HEADERS) {
			previousBlockHeaders.removeFirst();
		}
		// And add the current one
		previousBlockHeaders.addLast(i__newBuffer.clone());
		
		return true;
	}
	
}
