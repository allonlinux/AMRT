package fr.free.allonlinux.amrt.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import fr.free.allonlinux.amrt.AMRT;

@SuppressWarnings("serial")
public class StartPanel extends JPanel implements ActionListener
{
	MainFrame window;
	JButton imagePathButton;
	JButton outputPathButton;
	JButton next;
	ButtonGroup sdCardSizeGroup;

	public StartPanel(MainFrame i__window)
	{
		this.window=i__window;
		
		BorderLayout mainLayout = new BorderLayout();
		mainLayout.setVgap(20);
		setLayout(mainLayout);

		// == PAGE_START ==
		// - Set title
		add(new TitlePanel("Step 1 of 3"), BorderLayout.PAGE_START);

		// == CENTER ==
		JPanel center = new JPanel();
		center.setLayout(new GridBagLayout());
		// - Image Path
		if (AMRT.g__inputFile == null)
			imagePathButton = new JButton("[Click here]");
		else
			imagePathButton = new JButton(AMRT.g__inputFile);	
		imagePathButton.setPreferredSize(new Dimension(400, 25));
		imagePathButton.addActionListener(this);
		// - Output Path
		if (AMRT.g__outputDirectory == null)
			outputPathButton = new JButton("[Click here]");
		else
			outputPathButton = new JButton(AMRT.g__outputDirectory);
		outputPathButton.setPreferredSize(new Dimension(400, 25));
		outputPathButton.addActionListener(this);
		// - SD Card size
		//   - Create the radio buttons.
		JRadioButton SDCard32 = new JRadioButton("32 GB or less");
		SDCard32.setActionCommand("0x8000");
		JRadioButton SDCard64 = new JRadioButton("64 GB or more");
		SDCard64.setActionCommand("0x20000");
		if ( AMRT.g__filesystemClusterSize == 0x8000 )
			SDCard32.setSelected(true);
		else
			SDCard64.setSelected(true);
		// JRadioButton birdButton = new JRadioButton("Auto");
		// birdButton.setActionCommand("0x8000");
		//   - Group the radio buttons.
		sdCardSizeGroup = new ButtonGroup();
		sdCardSizeGroup.add(SDCard32);
		sdCardSizeGroup.add(SDCard64);
		JPanel buttonPanel = new JPanel(new GridLayout(1, 0));
		buttonPanel.add(SDCard32);
		buttonPanel.add(SDCard64);
		// - Set layout
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 10, 10, 20);
		c.gridx = 0;
		c.gridy = 0;
		center.add(new JLabel("Select input SD Card image file : "), c);
		c.gridx = 1;
		c.gridy = 0;
		center.add(imagePathButton, c);
		c.gridx = 0;
		c.gridy = 1;
		center.add(new JLabel("Select output directory : "), c);
		c.gridx = 1;
		c.gridy = 1;
		center.add(outputPathButton, c);
		c.gridx = 0;
		c.gridy = 2;
		center.add(new JLabel("Select your SD Card size : "), c);
		c.gridx = 1;
		c.gridy = 2;
		center.add(buttonPanel, c);
		// - add to main panel
		add(center, BorderLayout.CENTER);

		// == PAGE_END ==
		// Next button
		JPanel end = new JPanel();
		next = new JButton("Next");
		next.addActionListener(this);
		end.add(next);
		add(end, BorderLayout.PAGE_END);
	}

	public void actionPerformed(ActionEvent i__event)
	{
		// Handle open button action.
		JFileChooser fileChooser = new JFileChooser();
		if (i__event.getSource() == imagePathButton)
		{
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int returnVal = fileChooser.showOpenDialog(StartPanel.this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = fileChooser.getSelectedFile();
				AMRT.g__inputFile=file.getAbsolutePath();
				imagePathButton.setText(AMRT.g__inputFile);
			}
		}
		
		// Handle save button action.
		else if (i__event.getSource() == outputPathButton)
		{
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = fileChooser.showOpenDialog(StartPanel.this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = fileChooser.getSelectedFile();
				AMRT.g__outputDirectory=file.getAbsolutePath()+"/";
				outputPathButton.setText(AMRT.g__outputDirectory);
			}
		}	
		
		// Handle next button action.
		else if (i__event.getSource() == next)
		{
			// Check that the user selected the file and directory path
			if ( AMRT.g__inputFile == null || AMRT.g__outputDirectory == null )
			{
				JOptionPane.showMessageDialog(window, "You MUST select an input file and an output directory first");
				return;
			}
			
			// Get the SD Card size
			if ( sdCardSizeGroup.getSelection().getActionCommand().equals("0x8000") )
				AMRT.g__filesystemClusterSize=0x8000;
			else
				AMRT.g__filesystemClusterSize=0x20000;
			
			// Go to the next UI page and start the detection
			window.cardLayout.next(window.panels);
			AMRT.g__skipPreviousDetection=false;
			window.detectionPanel.detect();
		}
	}

}
