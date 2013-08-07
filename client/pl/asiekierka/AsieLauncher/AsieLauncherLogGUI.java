package pl.asiekierka.AsieLauncher;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.*;

public class AsieLauncherLogGUI extends JFrame {
	private static final long serialVersionUID = 6375185686352264799L;
	private JPanel panel;
	private JScrollPane pane;
	private JTextArea textArea;
	
	public AsieLauncherLogGUI() {
		setTitle(Strings.INSTALL_LOG);
		setResizable(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		panel = new JPanel();
		getContentPane().add(panel);
		panel.setLayout(new BorderLayout(8,8));
		textArea = new JTextArea(15, 50);
		pane = new JScrollPane(textArea);
		panel.setPreferredSize(new Dimension(450, (int)Math.round(450/1.61)));
		textArea.setEditable(false);
		panel.add(pane, BorderLayout.CENTER);
		pack();
		validate();
	}
	
	public void showLog(String[] logs) {
		textArea.setText("");
		for(String log: logs)
			textArea.append(log+'\n');
		setVisible(true);
	}
}
