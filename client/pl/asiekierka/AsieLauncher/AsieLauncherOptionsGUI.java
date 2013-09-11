package pl.asiekierka.AsieLauncher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
	public static final int OPTIONS_VERSION = 2;
	private static final long serialVersionUID = 1079662238420276795L;
	private JPanel panel;
	private HashMap<String, JCheckBox> optionBoxes;
	private HashMap<JCheckBox, String> optionBoxIDs;
	private JButton quitButton, logButton;
	protected JCheckBox loginCheckbox;
	public String filename;
	public ArrayList<String> oldOptions;
	public ArrayList<String> options;
	private JTextField ramAmount, otherArgs;
	private AsieLauncherGUI lgui;
	
	public AsieLauncherOptionsGUI(AsieLauncherGUI parent, Map<String, JSONObject> optionMap, String fn) {
		lgui = parent;
		filename=fn;
		setTitle(Strings.OPTIONS);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		panel = new JPanel();
		getContentPane().add(panel);
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(gbl);
		panel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		optionBoxes = new HashMap<String, JCheckBox>(optionMap.size());
		optionBoxIDs = new HashMap<JCheckBox, String>(optionMap.size());
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.gridx = 0;
		c.gridy = 0;
		for(String optionID : optionMap.keySet()) {
			JSONObject option = optionMap.get(optionID);
			JCheckBox box = new JCheckBox((String)option.get("name"), (Boolean)option.get("default"));
			box.setToolTipText((String)option.get("description"));
			panel.add(box, c);
			optionBoxes.put(optionID, box);
			optionBoxIDs.put(box, optionID);
			c.gridy++;
		}
		
		ramAmount = new JTextField("1024", 5);
		otherArgs = new JTextField(20);
		//JPanel innerPanel = new JPanel();
		//panel.add(innerPanel);
		logButton = new JButton(Strings.SHOW_INSTALL_LOG);
		logButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				setOptions();
				AsieLauncherLogGUI logGUI = new AsieLauncherLogGUI();
				logGUI.showLog(lgui.generateLogs());
			}
		});
		quitButton = new JButton(Strings.OK);
	    quitButton.addActionListener(new ActionListener() {
	    	@Override
	        public void actionPerformed(ActionEvent event) {
	    		setOptions();
	        	setVisible(false);
	        }
	    });
	    loginCheckbox = new JCheckBox("Keep logged in");
	    // Warning: The part below is uuuuugly.
	    // Don't tell me I didn't warn you.
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridy++;
		panel.add(new JLabel(Strings.RAM_AMOUNT_MB), c);
		c.gridy++;
		panel.add(new JLabel(Strings.OTHER_JVM_ARGS), c);
		c.gridy++;
		if(lgui.canKeepPassword()) {
			c.gridy++;
		}
	    panel.add(logButton, c);
	    c.anchor = GridBagConstraints.LINE_END;
	    c.gridx++;
	    c.gridy-=2;
	    if(lgui.canKeepPassword()) {
	    	c.gridy--;
	    }
		panel.add(ramAmount, c);
		c.gridy++;
		panel.add(otherArgs, c);
		c.gridy++;
		if(lgui.canKeepPassword()) {
			panel.add(loginCheckbox, c);
			c.gridy++;
		}
		panel.add(quitButton, c);
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
	
	public void setDefaultArgs(String args) {
		if(otherArgs.getText().length() <= 1) otherArgs.setText(args);
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
			if(currentVersion > 1) {
				loginCheckbox.setEnabled(new Boolean(reader.readLine()));
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
			writer.write((loginCheckbox.isSelected() ? "true" : "false") + '\n');
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
