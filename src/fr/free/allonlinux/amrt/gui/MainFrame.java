package fr.free.allonlinux.amrt.gui;

import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class MainFrame  extends JFrame
{
	
	public CardLayout cardLayout;
	public JPanel panels;
	public StartPanel startPanel;
	public DetectionPanel detectionPanel;
	public ResultPanel resultPanel;
	
	public MainFrame()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("AMRT");
		
		cardLayout=new CardLayout();
		panels = new JPanel(cardLayout);
		panels.add(startPanel=new StartPanel(this));
		panels.add(detectionPanel=new DetectionPanel(this));
		panels.add(resultPanel=new ResultPanel(this));
		panels.setBorder(new EmptyBorder(10, 20, 10, 20) );
		add(panels);
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
