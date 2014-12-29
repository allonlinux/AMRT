package fr.free.allonlinux.amrt.media;

import java.util.regex.Pattern;


public class MediaDescriptor implements java.io.Serializable
{
	private static final long serialVersionUID = 6639713951645430943L;
	
	public MediaTypeEnum type;
	public MediaCodecEnum codec;
	public Pattern pattern;
	
	public MediaDescriptor(MediaTypeEnum i__type, MediaCodecEnum i__subType, String i__pattern)
	{
		type=i__type;
		codec=i__subType;
		if ( i__pattern != null )
			pattern=Pattern.compile(i__pattern);
	}
	
}
