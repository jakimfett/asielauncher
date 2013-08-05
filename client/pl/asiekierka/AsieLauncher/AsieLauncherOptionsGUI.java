package pl.asiekierka.AsieLauncher;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import javax.swing.*;

import org.json.simple.*;

public class AsieLauncherOptionsGUI extends JFrame {
	private static final long serialVersionUID = 1079662238420276795L;
	private JPanel panel;
	private HashMap<String, JCheckBox> optionBoxes;
	private HashMap<JCheckBox, String> optionBoxIDs;
	private JButton quitButton;
	private String filename;
	
	public AsieLauncherOptionsGUI(Map<String, JSONObject> options, String fn) {
		filename=fn;
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
		optionBoxIDs = new HashMap<JCheckBox, String>(options.size());
		for(String optionID : options.keySet()) {
			JSONObject option = options.get(optionID);
			JCheckBox box = new JCheckBox((String)option.get("name"), (Boolean)option.get("default"));
			box.setToolTipText((String)option.get("description"));
			panel.add(box);
			optionBoxes.put(optionID, box);
			optionBoxIDs.put(box, optionID);
		}
		quitButton = new JButton(Strings.OK);
		quitButton.setAlignmentX(RIGHT_ALIGNMENT);
	    quitButton.addActionListener(new ActionListener() {
	    	@Override
	        public void actionPerformed(ActionEvent event) {
	    		saveSelectedOptions(filename);
	        	setVisible(false);
	        }
	    });
		panel.add(quitButton);
		loadSelectedOptions(filename);
		pack();
		validate();
	}
	
	public void loadSelectedOptions(String filename) {
		String line = "";
		File file = new File(filename);
		if(!file.exists()) return;
		// Reset all checkboxes
		for(JCheckBox box: optionBoxes.values()) {
			box.setSelected(false);
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			line = reader.readLine();
			while(line != null) {
				JCheckBox box = optionBoxes.get(line);
				if(box != null) box.setSelected(true);
				line = reader.readLine();
			}
			reader.close();
		}
		catch(Exception e) { e.printStackTrace(); }
		return;
	}
	
	public void saveSelectedOptions(String filename) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			for(JCheckBox box: optionBoxes.values()) {
				if(box != null && box.isSelected()) writer.write(optionBoxIDs.get(box) + '\n');
			}
			writer.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			if(writer != null) try{writer.close();}catch(Exception ee){}
			return;
		}
		return;
	}
}
