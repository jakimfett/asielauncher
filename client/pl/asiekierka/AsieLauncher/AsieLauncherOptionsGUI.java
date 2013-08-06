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
	public static final int OPTIONS_VERSION = 1;
	private static final long serialVersionUID = 1079662238420276795L;
	private JPanel panel;
	private HashMap<String, JCheckBox> optionBoxes;
	private HashMap<JCheckBox, String> optionBoxIDs;
	private JButton quitButton;
	public String filename;
	public ArrayList<String> oldOptions;
	public ArrayList<String> options;
	private JTextField ramAmount, otherArgs;
	
	public AsieLauncherOptionsGUI(Map<String, JSONObject> optionMap, String fn) {
		filename=fn;
		setTitle(Strings.OPTIONS);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		panel = new JPanel();
		getContentPane().add(panel);
		panel.setLayout(new GridLayout(optionMap.size()+2, 1));
		panel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		optionBoxes = new HashMap<String, JCheckBox>(optionMap.size());
		optionBoxIDs = new HashMap<JCheckBox, String>(optionMap.size());
		for(String optionID : optionMap.keySet()) {
			JSONObject option = optionMap.get(optionID);
			JCheckBox box = new JCheckBox((String)option.get("name"), (Boolean)option.get("default"));
			box.setToolTipText((String)option.get("description"));
			panel.add(box);
			optionBoxes.put(optionID, box);
			optionBoxIDs.put(box, optionID);
		}
		
		ramAmount = new JTextField("1024", 5);
		otherArgs = new JTextField(20);
		JPanel innerPanel = new JPanel();
		panel.add(innerPanel);
		innerPanel.setLayout(new GridLayout(2,2));
		innerPanel.add(new JLabel(Strings.RAM_AMOUNT_MB));
		innerPanel.add(ramAmount);
		innerPanel.add(new JLabel(Strings.OTHER_JVM_ARGS));
		innerPanel.add(otherArgs);
		quitButton = new JButton(Strings.OK);
	    quitButton.addActionListener(new ActionListener() {
	    	@Override
	        public void actionPerformed(ActionEvent event) {
	    		setOptions();
	        	setVisible(false);
	        }
	    });
		panel.add(quitButton);
		loadSelectedOptions(filename);
		oldOptions = getOptions();
		options = getOptions();
		pack();
		validate();
	}
	
	public String getJVMArgs() {
		int ramSize = new Integer(ramAmount.getText().trim());
		if(ramSize == 0) ramSize = 640;
		if(ramSize < 320) ramSize = 320;
		if(otherArgs.getText().length() > 0) {
			return "-Xmx"+ramSize+"m -Xms"+ramSize+"m "+otherArgs.getText();
		} else {
			return "-Xmx"+ramSize+"m -Xms"+ramSize+"m";
		}
	}
	
	public ArrayList<String> getOptions() {
		ArrayList<String> options = new ArrayList<String>();
		for(JCheckBox box: optionBoxes.values()) {
			if(box.isSelected()) options.add(optionBoxIDs.get(box));
		}
		return options;
	}
	
	public void setOptions() {
		options = getOptions();
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
			int currentVersion = new Integer(line);
			if(currentVersion == 0) { reader.close(); return; }
			if(currentVersion > 0) {
				ramAmount.setText(reader.readLine());
				otherArgs.setText(reader.readLine());
			}
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
			writer.write(("" + OPTIONS_VERSION) + '\n');
			writer.write(ramAmount.getText() + '\n');
			writer.write(otherArgs.getText() + '\n');
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
