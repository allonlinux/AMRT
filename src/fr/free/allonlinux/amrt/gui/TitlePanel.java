package fr.free.allonlinux.amrt.gui;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class TitlePanel extends JPanel
{
	public TitlePanel(String i__title) {
		JLabel label = new JLabel(i__title);
		label.setFont(new Font(label.getFont().getFontName(), Font.BOLD, label.getFont().getSize() + 8));
		add(label);
	}

}
