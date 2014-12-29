package fr.free.allonlinux.amrt;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import fr.free.allonlinux.amrt.gui.MainFrame;
import fr.free.allonlinux.amrt.media.MediaCodecEnum;
import fr.free.allonlinux.amrt.media.MediaDescriptor;
import fr.free.allonlinux.amrt.media.MediaTypeEnum;
import fr.free.allonlinux.amrt.utils.LogFormatter;

public class AMRT
{
	public final static Logger g__log = Logger.getLogger(AMRT.class .getName());
	public static Level g__logLevel =Level.INFO; 
	
	/** Cluster size, depending on the SD Card size :
	 * - if size <= 32GB : 0x8000
	 * - if size > 32GB : 0x20000
	 */
	public static int g__filesystemClusterSize=0x8000; 
	
	/** List of headers that will be detected */
	public static MediaDescriptor g__headers[];

	/** Input file */
	public static String g__inputFile;
	/** Output directory */
	public static String g__outputDirectory;
	/** */
	public static boolean g__skipPreviousDetection=false;
	public static long g__lastHeaderOffset=0;
	
	/**
	 * Create the media header array from the patterns that can be used to identify them
	 */
	static
	{
		g__headers=new MediaDescriptor[4];
		g__headers[0]=new MediaDescriptor(MediaTypeEnum.Video,	MediaCodecEnum.MP4_mp42,		"\\x00\\x00\\x00\\x20\\x66\\x74\\x79\\x70\\x6D\\x70\\x34\\x32\\x00\\x00\\x00\\x00");
		g__headers[1]=new MediaDescriptor(MediaTypeEnum.Video,	MediaCodecEnum.MP4_avc1,		"\\x00\\x00\\x00\\x20\\x66\\x74\\x79\\x70\\x61\\x76\\x63\\x31\\x00\\x00\\x00\\x00");
		g__headers[2]=new MediaDescriptor(MediaTypeEnum.Image,	MediaCodecEnum.THM,			"\\xFF\\xD8\\xFF\\xE0\\x00\\x10\\x4A\\x46\\x49\\x46");
		g__headers[3]=new MediaDescriptor(MediaTypeEnum.Image,	MediaCodecEnum.JPEG,			"\\xFF\\xD8\\xFF\\xE1..\\x45\\x78\\x69\\x66\\x00\\x00\\x4D\\x4D\\x00\\x2A");
	}

	private static void usage()
	{
		g__log.log(Level.ALL,"Usage :");
		g__log.log(Level.ALL,"\tjava -jar AMRT [-inputFile=PATH] [-outputDir=PATH] [-clusterSize=HEX_VALUE]");
		g__log.log(Level.ALL,"");
		g__log.log(Level.ALL,"-inputFile : PATH to the recovered image of you GoPro SD Card");
		g__log.log(Level.ALL,"-outputDir : PATH to the output directory where the recovered files will be saved");
		g__log.log(Level.ALL,"-clusterSize : cluster size in hex format (default='0x8000'). For SD Card > 32GB, use '0x20000' ");
		g__log.log(Level.ALL,"");
		g__log.log(Level.ALL,"Note : requires a version of Java >= 1.6");
		g__log.log(Level.ALL,"");
	}
	
	public static void main(String args[])
	{
		// Check input parameters
		int l__nbParams=args.length;
		while ( l__nbParams > 0 )
		{
			String l__param=args[args.length-l__nbParams];
			
			if ( l__param.startsWith("-inputFile=") )
			{
				g__inputFile=l__param.substring("-inputFile=".length());
				if ( ! (new File(g__inputFile)).isFile() )
				{
					g__log.log(Level.SEVERE,"The input file you specified doesn't exist : '"+g__inputFile+"'");
					return;
				}
			}
			else if ( l__param.startsWith("-outputDir=") )
			{
				g__outputDirectory=l__param.substring("-outputDir=".length());
				if ( ! (new File(g__outputDirectory)).isDirectory() )
				{
					g__log.log(Level.SEVERE,"The output directory you specified doesn't exist : '"+g__outputDirectory+"'");
					return;
				}
				g__outputDirectory+="/";
			}
			else if ( l__param.startsWith("-clusterSize=") )
			{
				g__filesystemClusterSize=Integer.decode(l__param.substring("-clusterSize=".length()));
			}
			else if ( l__param.startsWith("-logLevel=") )
			{
				String l__paramValue=l__param.substring("-logLevel=".length());
				if ( l__paramValue.equals("debug") )
					g__logLevel=Level.FINE;
				if ( l__paramValue.equals("info") )
					g__logLevel=Level.INFO;
				if ( l__paramValue.equals("warning") )
					g__logLevel=Level.WARNING;
			}
			else
			{
				g__log.log(Level.SEVERE,"Invalid option");
				usage();
				return;
			}
			
			l__nbParams--;
		}

		// Set log configuration
		LogManager.getLogManager().reset();
		Handler l__handler=new ConsoleHandler();
		l__handler.setLevel(g__logLevel);
		l__handler.setFormatter(new LogFormatter());
		g__log.setLevel(g__logLevel);
		g__log.addHandler(l__handler);
		
		// Print the parameters that will be used
		if ( g__inputFile!= null && g__outputDirectory!= null )
		{
			g__log.log(Level.INFO,"Parameters :");
			g__log.log(Level.INFO,"- input file : " + g__inputFile);
			g__log.log(Level.INFO,"- output directory : " + g__outputDirectory);
			g__log.log(Level.INFO,"- cluster size : 0x%x", g__filesystemClusterSize);
			g__log.log(Level.INFO,"\n");
		}
		
		new MainFrame();
	}
	
}
