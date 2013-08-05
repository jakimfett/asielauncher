package pl.asiekierka.AsieLauncher;

public class LauncherThread extends Thread {
	private AsieLauncher launcher;
	private String u, se;
	private boolean online;
	private AsieLauncherOptionsGUI options;
	public LauncherThread(AsieLauncher l, AsieLauncherOptionsGUI o, String username, String sessionID, boolean _online) {
		launcher = l;
		options = o;
		u = username;
		se = sessionID;
		online = _online;
	}

	public void run() {
		if(!online || launcher.install(options.options, options.oldOptions)) {
			launcher.launch(u,se,options.getJVMArgs());
		}
	}
}
