package fr.free.allonlinux.amrt;

import java.util.regex.Pattern;

public class MediaPattern implements java.io.Serializable {
	
	private static final long serialVersionUID = 6639713951645430943L;
	
	public MediaType type;
	public MediaCodec codec;
	public Pattern pattern;
	
	public MediaPattern(MediaType i__type, MediaCodec i__subType, String i__pattern) {
		type=i__type;
		codec=i__subType;
		pattern=Pattern.compile(i__pattern);
	}
	
}
