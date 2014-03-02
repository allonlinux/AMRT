package fr.free.allonlinux.amrt;


public class MediaOccurence implements java.io.Serializable {
	
	private static final long serialVersionUID = 2932859665986365882L;
	
	long offset;
	MediaPattern pattern;
	
	public MediaOccurence(long i__offset, MediaPattern i__pattern) {
		offset=i__offset;
		pattern=i__pattern;
	}
}
