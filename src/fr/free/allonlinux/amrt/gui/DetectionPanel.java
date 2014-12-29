package fr.free.allonlinux.amrt.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import fr.free.allonlinux.amrt.AMRT;
import fr.free.allonlinux.amrt.media.MediaDetector;
import fr.free.allonlinux.amrt.media.MediaItem;

@SuppressWarnings("serial")
public class DetectionPanel extends JPanel implements ActionListener
{
	MainFrame window;
	JTable table;
	JButton previous, next, detect;
	
	static ArrayList<MediaItem> g__medias;
	public static TableModel g__tableModel;
	
	public DetectionPanel(MainFrame i__window)
	{
		this.window=i__window;
		i__window.setMinimumSize(new Dimension(800,600));
		
		BorderLayout mainLayout = new BorderLayout();
		mainLayout.setVgap(20);
		mainLayout.setHgap(40);
		setLayout(mainLayout);
		
		// == PAGE_START ==
		// - Set title
		add(new TitlePanel("Step 2 of 3"), BorderLayout.PAGE_START);

		// == CENTER ==
		// - table
		BorderLayout centerLayout = new BorderLayout();
		centerLayout.setVgap(20);
		centerLayout.setHgap(40);
		JPanel center = new JPanel();
		center.setLayout(centerLayout);
		String[] columnNames = { "Media type", "Codec", "Position" };
		table = new JTable(new TableModel(columnNames));
		table.setAutoCreateRowSorter(true);
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	        //table.setPreferredScrollableViewportSize(new Dimension(500, 70));
	        table.setFillsViewportHeight(true);
	        // table.getSelectionModel().addListSelectionListener(new RowListener());
	        // table.getColumnModel().getSelectionModel().addListSelectionListener(new ColumnListener());
		center.add(new JScrollPane(table), BorderLayout.CENTER);
		// - lateral panel
		JPanel lineEnd = new JPanel();
		detect = new JButton("Restart detection");
		detect.addActionListener(this);
		lineEnd.add(detect);
		center.add(lineEnd, BorderLayout.LINE_END);
		add(center, BorderLayout.CENTER);
		
		// == PAGE_END ==
		// Next button
		JPanel endPanel = new JPanel();
		previous = new JButton("Previous");
		previous.addActionListener(this);
		next = new JButton("Next");
		next.setEnabled(false);
		next.addActionListener(this);
		endPanel.add(previous);
		endPanel.add(next);
		add(endPanel, BorderLayout.PAGE_END);
	}
	
	public void actionPerformed(ActionEvent i__event)
	{
		// Handle 'previous' button action.
		if (i__event.getSource() == previous)
		{
			window.cardLayout.previous(window.panels);
		}
		
		// Handle 'next' button action.
		else if (i__event.getSource() == next)
		{
			window.cardLayout.next(window.panels);
		}
		
		// Handle 'detect' button action.
		else if (i__event.getSource() == detect)
		{
			AMRT.g__skipPreviousDetection=true;
			detect();
			next.setEnabled(false);
			detect.setEnabled(false);
		}
	}
	
	public void detect()
	{
		// Clean previous detections
		TableModel tableModel = (TableModel) table.getModel();
		tableModel.removeAll();
		
		// Start detection
		(new DetectionThread(this)).start();
	}
	
	public class DetectionThread extends Thread
	{
		DetectionPanel detectionUI;
		
		public DetectionThread(DetectionPanel i__detectionUI)
		{
			detectionUI=i__detectionUI;
		}

		public void run()
		{
			g__tableModel = (TableModel) table.getModel();
			g__medias = MediaDetector.detect();

			next.setEnabled(true);
			detect.setEnabled(true);
		}

	}
	
}
