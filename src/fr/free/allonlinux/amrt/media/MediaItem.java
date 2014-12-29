package fr.free.allonlinux.amrt.media;

public class MediaItem implements java.io.Serializable
{
	private static final long serialVersionUID = 2932859665986365882L;
	
	public long offset;
	public MediaDescriptor descriptor;
	
	public MediaItem(long i__offset, MediaTypeEnum i__type)
	{
		this(i__offset, new MediaDescriptor(i__type, null, null));
	}
	
	public MediaItem(long i__offset, MediaDescriptor i__pattern)
	{
		offset=i__offset;
		descriptor=i__pattern;
	}
}
