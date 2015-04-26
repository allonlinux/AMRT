package fr.free.allonlinux.amrt.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fr.free.allonlinux.amrt.AMRT;
import fr.free.allonlinux.amrt.media.MediaCodecEnum;
import fr.free.allonlinux.amrt.media.MediaItem;
import fr.free.allonlinux.amrt.media.video.VideoDecoder;

@SuppressWarnings("serial")
public class ResultPanel extends JPanel implements ActionListener
{
	MainFrame window;
	JButton previous, process;
	JLabel message;
	ProcessingThread processingThread;
	
	public ResultPanel(MainFrame i__window)
	{
		this.window=i__window;
		
		BorderLayout mainLayout = new BorderLayout();
		mainLayout.setVgap(20);
		mainLayout.setHgap(40);
		setLayout(mainLayout);
		
		// == PAGE_START ==
		// - Set title
		add(new TitlePanel("Step 3 of 3"), BorderLayout.PAGE_START);

		// == CENTER ==
		JPanel center = new JPanel();
		process= new JButton("Start recovering");
		process.addActionListener(this);
		message=new JLabel();
		message.setFont(new Font(message.getFont().getFontName(), Font.BOLD, message.getFont().getSize() + 8));
		center.add(process);
		center.add(message);
		add(center, BorderLayout.CENTER);
		
		
		// == PAGE_END ==
		// Next button
		JPanel endPanel = new JPanel();
		previous = new JButton("Previous");
		previous.addActionListener(this);
		endPanel.add(previous);
		add(endPanel, BorderLayout.PAGE_END);
	}
	
	public void actionPerformed(ActionEvent i__event)
	{
		// Handle 'previous' button action.
		if (i__event.getSource() == previous)
		{
			// Stop any running process
			if (processingThread!=null && processingThread.isAlive())
			{
				processingThread.interrupt();
			}
			window.cardLayout.previous(window.panels);
		}
		// Handle 'process' button action.
		else if (i__event.getSource() == process)
		{
			// Start processing
			process();
			process.setEnabled(false);
			process.setVisible(false);
		}
	}
	
	public void process()
	{
		// Start processing
		processingThread=new ProcessingThread(this);
		processingThread.start();
	}
	
	public class ProcessingThread extends Thread
	{
		ResultPanel resultUI;
		
		public ProcessingThread(ResultPanel i__resultUI)
		{
			resultUI=i__resultUI;
		}

		public void run()
		{
			RandomAccessFile l__file=null;
			try
			{
				// Open file
				l__file = new RandomAccessFile(AMRT.g__inputFile, "r");
				FileChannel l__channel=l__file.getChannel();
				
				// Process detected medias
				MediaItem l__medias[]=DetectionPanel.g__medias.toArray(new MediaItem[DetectionPanel.g__medias.size()]);
				for (int i=0;i<l__medias.length;i++)
				{
					message.setText("Processing : "+i+"/"+l__medias.length);
					
					// Update last offset value
					if ( i > 0 )
					{
						AMRT.g__lastHeaderOffset=l__medias[i-1].offset;
					}
					
					MediaCodecEnum l__codec=l__medias[i].descriptor.codec;
					
					switch( l__codec )
					{
						// Protune "OFF"
						case THM:
						case JPEG:
						{
							try {
								// Read at most 25MB from the recovery file
								// - compute reading size
								long l__readSize=25000000;
								if ( i < l__medias.length-1 )
									l__readSize=Math.min(l__medias[i+1].offset-l__medias[i].offset, l__readSize);
								// - read buffer from file
								ByteBuffer l__imageBuffer = ByteBuffer.allocate((int)l__readSize);
								l__imageBuffer.clear();
								l__channel.position(l__medias[i].offset);
								int l__size=l__channel.read(l__imageBuffer);
								AMRT.g__log.log(Level.FINE,"Image reading size from file = "+l__size);
								l__imageBuffer.rewind();
								// - try opening image
								BufferedImage l__javaImage=ImageIO.read(new ByteArrayInputStream(l__imageBuffer.array()));
								
								// Write output image using PNG format
								ImageIO.write(l__javaImage,"PNG", new File(AMRT.g__outputDirectory+"GOPRO__"+i+".PNG"));
								
								AMRT.g__log.log(Level.INFO,"Successfuly recovered image : "+"GOPRO__"+i+".PNG");
							} catch(Exception e) {
								AMRT.g__log.log(Level.SEVERE,"Failed recovering image... ");
							}
							
							// If : (THM or JPEG)/VIDEO_1/VIDEO_2
							if (	i+2 < l__medias.length && 
									(l__medias[i+1].descriptor.codec == MediaCodecEnum.MP4_mp42 || l__medias[i+1].descriptor.codec == MediaCodecEnum.MP4_avc1)  && 
									(l__medias[i+2].descriptor.codec == MediaCodecEnum.MP4_mp42 || l__medias[i+2].descriptor.codec == MediaCodecEnum.MP4_avc1) )
							{
								boolean l__result=VideoDecoder.recover(l__channel,l__medias[i],l__medias[i+1],l__medias[i+2]);
								if (l__result)
								{
									i+=2;
								}
							}
						} break;
						
						// Protune "ON"
						case MP4_mp42:
						case MP4_avc1:
						{
							// If : VIDEO_1/(THM or JPEG)/VIDEO_2
							if (	i+2 < l__medias.length && 
									(l__medias[i+1].descriptor.codec == MediaCodecEnum.THM || l__medias[i+1].descriptor.codec == MediaCodecEnum.JPEG) && 
									(l__medias[i+2].descriptor.codec == MediaCodecEnum.MP4_mp42 || l__medias[i+2].descriptor.codec == MediaCodecEnum.MP4_avc1) )
							{
								boolean l__result=VideoDecoder.recover(l__channel,l__medias[i+1],l__medias[i],l__medias[i+2]);
								if (l__result)
								{
									i+=2;
									continue;
								}
							}
							
							// If : VIDEO_1/VIDEO_2/(THM or JPEG)
							if (	i+2 < l__medias.length && 
									(l__medias[i+1].descriptor.codec == MediaCodecEnum.MP4_mp42 || l__medias[i+1].descriptor.codec == MediaCodecEnum.MP4_avc1)  && 
									(l__medias[i+2].descriptor.codec == MediaCodecEnum.THM || l__medias[i+2].descriptor.codec == MediaCodecEnum.JPEG) )
							{
								boolean l__result=VideoDecoder.recover(l__channel,l__medias[i+2],l__medias[i],l__medias[i+1]);
								if (l__result)
								{
									i+=2;
									continue;
								}
							}
							
							// If : VIDEO_1/VIDEO_2
							if (	i+1 < l__medias.length && 
									(l__medias[i].descriptor.codec == MediaCodecEnum.MP4_mp42 || l__medias[i].descriptor.codec == MediaCodecEnum.MP4_avc1)  && 
									(l__medias[i+1].descriptor.codec == MediaCodecEnum.MP4_mp42 || l__medias[i+1].descriptor.codec == MediaCodecEnum.MP4_avc1) )
							{
								boolean l__result=VideoDecoder.recover(l__channel, null,l__medias[i],l__medias[i+1]);
								if (l__result)
								{
									i+=1;
									continue;
								}
							}
							
							//If : VIDEO_1
							{
								VideoDecoder.recover(l__channel,l__medias[i]);
							}
						}  break;
						
						default:
						{
							AMRT.g__log.log(Level.INFO,"Media not handled yet... '"+l__codec+"'");
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				// Close file
				if (l__file != null )
				{
					try
					{
						l__file.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}	
			}
			
			message.setText("Processing finished");
		}
	}
}
