package pl.asiekierka.AsieLauncher;

public class LauncherThread extends Thread {
	private AsieLauncher launcher;
	private String u, pass;
	private boolean online;
	private AsieLauncherOptionsGUI options;
	public LauncherThread(AsieLauncher l, AsieLauncherOptionsGUI o, String username, String password, boolean _online) {
		launcher = l;
		options = o;
		u = username;
		pass = password;
		online = _online;
	}

	public void run() {
		if(!online || launcher.install(options.options, options.oldOptions)) {
			launcher.launch(u,pass,options.getJVMArgs());
		}
	}
}
