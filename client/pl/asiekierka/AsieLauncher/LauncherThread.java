package pl.asiekierka.AsieLauncher;

public class LauncherThread extends Thread {
	private AsieLauncher launcher;
	private String u, se;
	private boolean online;
	public LauncherThread(AsieLauncher l, String username, String sessionID, boolean _online) {
		launcher = l;
		u = username;
		se = sessionID;
		online = _online;
	}

	public void run() {
		if(!online || launcher.install()) {
			launcher.launch(u,se);
		}
	}
}
