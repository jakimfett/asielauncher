package pl.asiekierka.AsieLauncher;

import com.camick.BackgroundPanel;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.json.simple.*;

public class AsieLauncherGUI extends JFrame implements IProgressUpdater {
	private static final long serialVersionUID = 550781190397000747L;
	public boolean isRunning;
	private AsieLauncher launcher;
	private BackgroundPanel panel;
	private JButton quitButton, launchButton, optionsButton;
	private JLabel statusLabel, loginLabel;
	private JTextField loginField;
	private JProgressBar progressBar;
	private Image background;
	private AsieLauncherOptionsGUI options;
	public boolean hasInternet = true;
	
	public AsieLauncherGUI() {
		launcher = new AsieLauncher();
		launcher.updater = (IProgressUpdater)this;
		isRunning = true;
		setTitle("asieLauncher");
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent ev) {
				isRunning = false;
			}
		});
		background = getToolkit().getImage(getClass().getResource("/resources/background.png"));
		panel = new BackgroundPanel(background);
		panel.setTransparentAdd(false);
		getContentPane().setSize(320,240);
		getContentPane().setPreferredSize(new Dimension(320,240));
		getContentPane().setMaximumSize(new Dimension(320,240));
		getContentPane().setMinimumSize(new Dimension(320,240));
		getContentPane().add(panel);
		panel.setLayout(null);
		pack();
	}
	
	public boolean getLaunchedMinecraft() { return launcher.launchedMinecraft; }
	public boolean isActive() { return launcher.isActive(); }
	
	public boolean validateLaunch() {
		return (loginField.getText().length() > 1);
	}
	
	public void update(int progress, int finish) {
		progressBar.setMaximum(finish);
		progressBar.setValue(progress);
		setStatus(cStatus);
	}
	
	private String cStatus;
	
	public void setStatus(String status ){
		cStatus = status;
		int statusProgress = (int)Math.round(progressBar.getValue()*100.0/progressBar.getMaximum());
		statusLabel.setText("["+statusProgress + "%] " + status);
	}
	
	public void initGUILogin() {
	       quitButton = new JButton(Strings.QUIT);
	       quitButton.setBounds(245, 189, 65, 25);
	       quitButton.addActionListener(new ActionListener() {
	    	   @Override
	           public void actionPerformed(ActionEvent event) {
	        	   setVisible(false);
	               isRunning = false;
	           }
	       });
	       
	       optionsButton = new JButton(Strings.OPTIONS);
	       optionsButton.setBounds(8, 189, 76, 25);
	       optionsButton.addActionListener(new ActionListener() {
	    	   @Override
	           public void actionPerformed(ActionEvent event) {
	        	   options.setVisible(true);
	        	   options.repaint();
	           }
	       });
	       
	       launchButton = new JButton(Strings.LAUNCH_UPDATE);
	       if(!hasInternet) launchButton.setText(Strings.LAUNCH_ONLY);
	       launchButton.setBounds(90, 189, 149, 25);
	       launchButton.addActionListener(new ActionListener() {
	    	   @Override
	           public void actionPerformed(ActionEvent event) {
	    		   if(validateLaunch()) {
	    			   quitButton.setEnabled(false);
	    			   launchButton.setEnabled(false);
	    			   loginLabel.setText(Strings.PROGRESS+":");
	    		       loginLabel.setBounds(10, 162, 70, 15);
	    			   panel.remove(loginField);
	    			   AsieLauncher.saveString(launcher.directory + "nickname.txt", loginField.getText());
	    			   panel.add(progressBar);
	    			   statusLabel.setText(Strings.START_UPDATE);
	    			   repaint();
	    			   LauncherThread thread = new LauncherThread(launcher, loginField.getText(), "", hasInternet);
	    			   thread.start();
	    		   } else {
	    			   statusLabel.setText(Strings.INVALID_LOGIN);
	    			   repaint();
	    		   }
	           }
	       });
	       
	       statusLabel = new JLabel(Strings.READY);
	       statusLabel.setBounds(6, 219, 300, 15);
	       
	       loginLabel = new JLabel(Strings.LOGIN+":");
	       loginLabel.setBounds(10, 160, 50, 15);
	       
	       loginField = new JTextField();
	       loginField.setBounds(60, 156, 251, 24);
	       loginField.setText(AsieLauncher.loadString(launcher.directory + "nickname.txt"));
	       progressBar = new JProgressBar();
	       progressBar.setBounds(86, 160, 224, 20);
	       
	       panel.add(launchButton);
	       panel.add(optionsButton);
	       panel.add(quitButton);
	       panel.add(statusLabel);
	       panel.add(loginLabel);
	       panel.add(loginField);
		   repaint();
	}
	public boolean init() {
		boolean linit = launcher.init();
		options = new AsieLauncherOptionsGUI(launcher.getOptionMap());
		if(!linit) hasInternet = false;
		setVisible(true);
		initGUILogin();
		return true;
	}
}
