package pl.asiekierka.AsieLauncher;

import com.camick.BackgroundPanel;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class AsieLauncherGUI extends JFrame implements IProgressUpdater {
	private static final long serialVersionUID = 550781190397000747L;
	public boolean isRunning;
	private AsieLauncher launcher;
	private BackgroundPanel panel;
	private JButton quitButton, launchButton, optionsButton;
	private JLabel statusLabel, loginLabel, passwordLabel;
	private JTextField loginField;
	private JPasswordField passwordField;
	private JProgressBar progressBar;
	private Image background;
	private AsieLauncherOptionsGUI options;
	public boolean hasInternet = true;
	private double scaleFactor;
	private boolean controlDown = false;
	
	private void setControl(boolean c) {
		controlDown = c;
	    if(!controlDown && hasInternet) launchButton.setText(Strings.LAUNCH_UPDATE);
	    else launchButton.setText(Strings.LAUNCH_ONLY);
	}
	
	public AsieLauncherGUI() {
		scaleFactor = Utils.getScaleFactor();
		launcher = new AsieLauncher();
		launcher.updater = (IProgressUpdater)this;
		isRunning = true;
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent ev) {
				isRunning = false;
			}
		});
		boolean has2x = getClass().getResource("/resources/background@2x.png") != null;
		if(!has2x || scaleFactor <= 1.0) background = getToolkit().getImage(getClass().getResource("/resources/background.png"));
		else background = getToolkit().getImage(getClass().getResource("/resources/background@2x.png"));
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

	       this.addKeyListener(new KeyListener() {
	    	   @Override
	    	   public void keyTyped(KeyEvent event) { }
	    	   @Override
	    	   public void keyPressed(KeyEvent event) {
	    		   if(event.getKeyCode() == KeyEvent.VK_CONTROL) setControl(true);
	    	   }
	    	   @Override
	    	   public void keyReleased(KeyEvent event) {
	    		   if(event.getKeyCode() == KeyEvent.VK_CONTROL) setControl(false);
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
	    			   String password = "";
	    			   if(passwordField != null) {
	    				   password = new String(passwordField.getPassword());
	    				   panel.remove(passwordLabel);
	    				   panel.remove(passwordField);
	    			   }
	    			   AsieLauncher.saveString(launcher.directory + "nickname.txt", loginField.getText());
	    			   options.saveSelectedOptions(options.filename);
	    			   panel.add(progressBar);
	    			   statusLabel.setText(Strings.START_UPDATE);
	    			   repaint();
	    			   LauncherThread thread = new LauncherThread(launcher, options, loginField.getText(), password, hasInternet && !controlDown);
	    			   thread.start();
	    		   } else {
	    			   statusLabel.setText(Strings.INVALID_LOGIN);
	    			   repaint();
	    		   }
	           }
	       });
	       
	       statusLabel = new JLabel(Strings.READY + " ("+Strings.VERSION+": "+AsieLauncher.VERSION_STRING+")");
	       statusLabel.setBounds(6, 219, 300, 15);
	       
	       loginLabel = new JLabel(Strings.LOGIN+":");
	       loginField = new JTextField();
	       loginField.setText(AsieLauncher.loadString(launcher.directory + "nickname.txt"));
	       
	       if(launcher.isOnlineMode()) {
	    	   passwordLabel = new JLabel(Strings.PASSWORD+":");
	    	   passwordField = new JPasswordField();
		       passwordLabel.setBounds(10, 160, 70, 15);
		       passwordField.setBounds(80, 156, 231, 24); 
		       loginLabel.setBounds(10, 134, 50, 15);
		       loginField.setBounds(80, 130, 231, 24); 
		       panel.add(passwordLabel);
		       panel.add(passwordField);
	       } else {
		       loginLabel.setBounds(10, 160, 50, 15);
		       loginField.setBounds(60, 156, 251, 24); 
	       }
	       
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
	public String[] generateLogs() {
		launcher.install(options.options, options.oldOptions, true);
		return launcher.getInstallLog();
	}
	public boolean init() {
		boolean linit = launcher.init();
		setTitle("AsieLauncher - " + launcher.WINDOW_NAME);
		options = new AsieLauncherOptionsGUI(this, launcher.getOptionMap(), launcher.directory + "also-options.txt");
		if(!linit) hasInternet = false;
		setVisible(true);
		initGUILogin();
		if(!launcher.sameClientRevision()) {
			JOptionPane.showMessageDialog(this, Strings.WRONG_CLIENT_REVISION);
		}
		return true;
	}
}
