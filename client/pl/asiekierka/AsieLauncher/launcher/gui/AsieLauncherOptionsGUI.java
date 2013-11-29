package pl.asiekierka.AsieLauncher.launcher.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import javax.swing.*;

import org.json.simple.*;

import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class AsieLauncherOptionsGUI extends JFrame {
	public static final int OPTIONS_VERSION = 2;
	private static final long serialVersionUID = 1079662238420276795L;
	private JPanel panel;
	private HashMap<String, JCheckBox> optionBoxes;
	private HashMap<JCheckBox, String> optionBoxIDs;
	private JButton quitButton, logButton, purgeButton;
	protected JCheckBox loginCheckbox;
	public String filename, filenameJSON;
	public ArrayList<String> oldOptions, options;
	private JTextField ramAmount, otherArgs;
	private AsieLauncherGUI lgui;
	
	public AsieLauncherOptionsGUI(AsieLauncherGUI parent, Map<String, JSONObject> optionMap, String fn) {
		lgui = parent;
		filename=fn;
		filenameJSON = fn.replaceAll("txt", "json");
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
		purgeButton = new JButton(Strings.PURGE);
		purgeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				int result = JOptionPane.showConfirmDialog(AsieLauncherOptionsGUI.this, Strings.PURGE_WARNING);
				if(result == 0) {
					// Kill it.
					try {
						Utils.deleteDirectory(new File(lgui.launcher.baseDir));
					} catch(Exception e) {
						JOptionPane.showMessageDialog(AsieLauncherOptionsGUI.this, e.getMessage());
						System.exit(1);
					}
					JOptionPane.showMessageDialog(AsieLauncherOptionsGUI.this, Strings.RESTART_MESSAGE);
					System.exit(0);
				}
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
	    loginCheckbox = new JCheckBox(Strings.KEEP_LOGGED_IN);
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
	    c.gridy++;
	    panel.add(purgeButton, c);
	    c.anchor = GridBagConstraints.LINE_END;
	    c.gridx++;
	    c.gridy-=3;
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
		c.gridy++;
		panel.add(quitButton, c);
		loadSelectedOptions();
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
	
	public void loadSelectedOptions() {
		File fileJSON = new File(filenameJSON);
		if(fileJSON.exists()) {
			loadSelectedOptionsJSON();
		}
		File file = new File(filename);
		if(!file.exists()) return; // No JSON, no TXT
		loadSelectedOptionsOld(file);
	}
	
	private void loadSelectedOptionsOld(File file) {
		String line = "";
		// Outdated code!
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
				loginCheckbox.setSelected(new Boolean(reader.readLine()));
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
	
	private void loadSelectedOptionsJSON() {
		JSONObject json = Utils.readJSONFile(filenameJSON);
		try {
			int version = ((Long)json.get("version")).intValue();
			switch(version) {
				case 1:
					ramAmount.setText((String)json.get("ramAmount"));
					otherArgs.setText((String)json.get("jvmArguments"));
					loginCheckbox.setSelected((Boolean)json.get("keepLoggedIn"));
					JSONObject options = (JSONObject)json.get("options");
					for(Object o: options.keySet()) {
						String s = (String)o;
						JCheckBox box = optionBoxes.get(s);
						if(box != null) box.setSelected((Boolean)options.get(s));
					}
				default:
					break;
			}
		} catch(NullPointerException e) {
			return;
		}
	}

	@SuppressWarnings("unchecked")
	public void saveSelectedOptions() {
		Utils.deleteIfExists(new File(filename));
		JSONObject config = new JSONObject();
		config.put("version", 1);
		config.put("ramAmount", ramAmount.getText());
		config.put("jvmArguments", otherArgs.getText());
		config.put("keepLoggedIn", loginCheckbox.isSelected());
		
		JSONObject boxes = new JSONObject();
		for(JCheckBox box: optionBoxes.values()) {
			if(box != null) boxes.put(optionBoxIDs.get(box), box.isSelected());
		}
		config.put("options", boxes);
		
		try {
			Utils.saveStringToFile(filenameJSON, config.toJSONString());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
