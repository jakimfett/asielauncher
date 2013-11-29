package pl.asiekierka.AsieLauncher.launcher.gui;

import pl.asiekierka.AsieLauncher.launcher.AsieLauncher;

public class LauncherThread extends Thread {
	private AsieLauncher launcher;
	private String u, pass;
	private boolean online;
	private AsieLauncherOptionsGUI options;
	private AsieLauncherGUI gui;
	
	public LauncherThread(AsieLauncherGUI gui, AsieLauncher l, AsieLauncherOptionsGUI o, String username, String password, boolean _online) {
		launcher = l;
		options = o;
		u = username;
		pass = password;
		online = _online;
		this.gui = gui;
	}

	public void run() {
		if(online && launcher.authenticate(u, pass)) {
			if(!online || launcher.install(options.options, options.oldOptions, false)) {
				launcher.launch(u,pass,options.getJVMArgs());
			}
		} else {
			gui.reinstateLoginBox(); // Login failed, give the player another chance
		}
	}
}
