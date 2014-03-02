package fr.free.allonlinux.amrt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class AMRT {
	
	/** Cluster size, depending on the SD Card size :
	 * - if size <= 32GB : 0x8000
	 * - if size > 32GB : 0x20000
	 */
	static public int FILESYSTEM_CLUSTER_SIZE=0x8000; 
	
	/** List of headers that will be detected */
	public static MediaPattern headers[];
	
	/** Output directory */
	static public String outputDirectory="./";
	
	/**
	 * Create the media header array from the patterns that can be used to identify them
	 */
	public static void createMediaHeaders() {
		headers=new MediaPattern[4];
		headers[0]=new MediaPattern(MediaType.Video,	MediaCodec.MP4_mp42, 	"\\x00\\x00\\x00\\x20\\x66\\x74\\x79\\x70\\x6D\\x70\\x34\\x32\\x00\\x00\\x00\\x00");
		headers[1]=new MediaPattern(MediaType.Video,	MediaCodec.MP4_avc1,		"\\x00\\x00\\x00\\x20\\x66\\x74\\x79\\x70\\x61\\x76\\x63\\x31\\x00\\x00\\x00\\x00");
		headers[2]=new MediaPattern(MediaType.Image,	MediaCodec.THM,				"\\xFF\\xD8\\xFF\\xE0\\x00\\x10\\x4A\\x46\\x49\\x46");
		headers[3]=new MediaPattern(MediaType.Image,	MediaCodec.JPEG,				"\\xFF\\xD8\\xFF\\xE1..\\x45\\x78\\x69\\x66\\x00\\x00\\x4D\\x4D\\x00\\x2A");
	}

	public static void usage() {
		System.out.println("Usage :");
		System.out.println("\tjava -jar AMRT -inputFile=PATH [-outputDir=PATH] [-clusterSize=HEX_VALUE]");
		System.out.println("");
		System.out.println("-inputFile : PATH to the recovered image of you GoPro SD Card");
		System.out.println("-outputDir : PATH to the output directory where the recovered files will be saved");
		System.out.println("-clusterSize : cluster size in hex format (default='0x8000'). For SD Card > 32GB, use '0x20000' ");
		System.out.println("");
		System.out.println("Note : requires a version of Java >= 1.6");
		System.out.println("");
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String args[]) {
		
		String l__inputFile=null;
		
		// Print usage if no arguments
		if ( args.length == 0 ) {
			usage();
		}
		
		// Check input parameters
		int l__nbParams=args.length;
		while ( l__nbParams > 0 ) {
			String l__param=args[args.length-l__nbParams];
			
			if ( l__param.startsWith("-inputFile=") ) {
				l__inputFile=l__param.substring("-inputFile=".length());
			}
			
			else if ( l__param.startsWith("-outputDir=") ) {
				outputDirectory=l__param.substring("-outputDir=".length());
				if ( ! (new File(outputDirectory)).isDirectory()) {
					System.out.println("The output directory you specified doesn't exist : '"+outputDirectory+"'");
					return;
				}
			}
			
			else if ( l__param.startsWith("-clusterSize=") ) {
				FILESYSTEM_CLUSTER_SIZE=Integer.decode(l__param.substring("-clusterSize=".length()));
			}
			
			else {
				System.out.println("Invalid option");
				usage();
				return;
			}
			
			l__nbParams--;
		}
		outputDirectory+="/";
		
		// If the input file is not specified, exit
		if (l__inputFile == null) {
			System.out.println("You must specify the input file to process");
			return;
		}
		
		// Print the parameters that will be used
		System.out.println("Parameters :");
		System.out.println("- input file : "+l__inputFile);
		System.out.println("- output directory : "+outputDirectory);
		System.out.printf("- cluster size : %x\n",FILESYSTEM_CLUSTER_SIZE);
		System.out.println("\n");
		
		// Create the list of medias to search given their binary header
		createMediaHeaders();
		
		RandomAccessFile l__file=null;
		try {
			// Open file
			l__file = new RandomAccessFile(l__inputFile, "r");
			FileChannel l__channel=l__file.getChannel();
			
			// Search for medias
			ArrayList<MediaOccurence> l__mediaList=null;
			File l__mediaListFile=new File(outputDirectory+"tmp_offsets.ser");
			// - check for previously found medias
			if ( l__mediaListFile.exists() ) {
				FileInputStream l__fileInputStrean = new FileInputStream(l__mediaListFile);
		        ObjectInputStream l__objectInputStream = new ObjectInputStream(l__fileInputStrean);
		        l__mediaList = (ArrayList<MediaOccurence>) l__objectInputStream.readObject();
		        l__objectInputStream.close();
		        l__fileInputStrean.close();
			}
			// - otherwise, find them and serialize the result
			else {
				// find
				l__mediaList=MediaFinder.find(l__channel);
				// serialize
				FileOutputStream l__fileOutputStream = new FileOutputStream(l__mediaListFile);
				ObjectOutputStream l__objectOutputStream = new ObjectOutputStream(l__fileOutputStream);
				l__objectOutputStream.writeObject(l__mediaList);
				l__objectOutputStream.close();
			}
			
			// Process detected medias
			MediaOccurence l__medias[]=l__mediaList.toArray(new MediaOccurence[l__mediaList.size()]);
			//for (int i=0;i<l__medias.length;i++) {
			for (int i=11;i<14;i++) {
				MediaCodec l__codec=l__medias[i].pattern.codec;
				
				switch( l__codec ) {
					// Protune "OFF"
					case THM: {
						// If the codec of the next two medias is 'MP4_avc1' 
						if (	i+2 < l__medias.length && 
								(l__medias[i+1].pattern.codec == MediaCodec.MP4_mp42 || l__medias[i+1].pattern.codec == MediaCodec.MP4_avc1)  && 
								(l__medias[i+2].pattern.codec == MediaCodec.MP4_mp42 || l__medias[i+2].pattern.codec == MediaCodec.MP4_avc1) ) {
							Video.recover(l__channel,l__medias[i],l__medias[i+1],l__medias[i+2]);
							i+=2;
						}
						break;
					}
					
					// Protune "ON"
					case MP4_mp42:
					case MP4_avc1:
					{
						// Two consecutive videos
						if (	i+1 < l__medias.length && 
								(l__medias[i].pattern.codec == MediaCodec.MP4_mp42 || l__medias[i].pattern.codec == MediaCodec.MP4_avc1)  && 
								(l__medias[i+1].pattern.codec == MediaCodec.MP4_mp42 || l__medias[i+1].pattern.codec == MediaCodec.MP4_avc1) ) {
							Video.recover(l__channel, null,l__medias[i],l__medias[i+1]);
							i+=2;
						}
						//Video.recover(l__channel,l__medias[i]);
						break;
					}
					
					default:
					{
						System.out.println("Media not handled yet... '"+l__codec+"'");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Close file
			if (l__file != null ) {
				try {
					l__file.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
		}
	}
	
}
