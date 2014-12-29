package fr.free.allonlinux.amrt.media.video;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import fr.free.allonlinux.amrt.media.MediaCodecEnum;
import fr.free.allonlinux.amrt.media.MediaItem;

public class Video
{
	// Video codec
	public MediaCodecEnum codec;
	
	// Video buffer is made of a header and some data
	public long headerSize;
	public long dataSize;
	
	// Video resolution
	public int videoWidth;
	public int videoHeight;
	
	// Video duration (nb frames + nb seconds)
	public int nbFrames;
	public long duration;
	
	// Video creation time
	public Date creationTime;
	
	public ArrayList<MediaItem> offsets=new ArrayList<MediaItem>();
	
	/**
	 * Convert the timestamp value in a printable date that will be used in the GUI and to create the output filename
	 */
	public String getCreationTime()
	{
		SimpleDateFormat l__dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
		l__dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return l__dateFormat.format(creationTime);
	}
	
	/**
	 * Print the information loaded from the header
	 */
	public String toString()
	{
		String l__result="Video :\n";
		l__result+="- codec :"+codec+"\n";
		l__result+="- size : "+videoWidth+"x"+videoHeight+"\n";
		l__result+="- duration : "+duration+" seconds\n";
		l__result+="- creationTime : "+getCreationTime()+"\n";
		l__result+="- nbFrames : "+nbFrames+"\n";
		l__result+="- headerSize : "+headerSize+"\n";
		l__result+="- dataSize : "+dataSize+"\n";
		
		return l__result;
	}
}
