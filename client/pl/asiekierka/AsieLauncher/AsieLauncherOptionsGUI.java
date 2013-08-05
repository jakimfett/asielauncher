package pl.asiekierka.AsieLauncher;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

import org.json.simple.*;

public class AsieLauncherOptionsGUI extends JFrame {
	private static final long serialVersionUID = 1079662238420276795L;
	private JPanel panel;
	private HashMap<String, JCheckBox> optionBoxes;
	private JButton quitButton;
	
	public AsieLauncherOptionsGUI(Map<String, JSONObject> options) {
		setTitle(Strings.OPTIONS);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		panel = new JPanel();
		getContentPane().add(panel);
		panel.setLayout(new GridLayout(options.size()+2, 1));
		panel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		JLabel label1 = new JLabel(Strings.OPTIONAL_MODS);
		panel.add(label1);
		optionBoxes = new HashMap<String, JCheckBox>(options.size());
		for(String optionID : options.keySet()) {
			JSONObject option = options.get(optionID);
			JCheckBox box = new JCheckBox((String)option.get("name"), (Boolean)option.get("default"));
			box.setToolTipText((String)option.get("description"));
			panel.add(box);
			optionBoxes.put(optionID, box);
		}
		quitButton = new JButton(Strings.OK);
		quitButton.setAlignmentX(RIGHT_ALIGNMENT);
	    quitButton.addActionListener(new ActionListener() {
	    	@Override
	        public void actionPerformed(ActionEvent event) {
	        	setVisible(false);
	        }
	    });
		panel.add(quitButton);
		pack();
		validate();
	}
}
