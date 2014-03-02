package fr.free.allonlinux.amrt;

import java.nio.channels.FileChannel;

public class VideoPlop {

	Video video;
	FileChannel channel;
	
	int blockIndex;						/** Current block index */
	long blockOffset; 					/** Absolute offset in the file */
	long blockSize;						/** Block size */
	
	public VideoPlop(Video i__video, FileChannel i__channel) {
		video=i__video;
		channel=i__channel;
		blockIndex=0;
	}
	
}
