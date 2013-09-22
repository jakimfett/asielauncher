package pl.asiekierka.AsieLauncher.launcher;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;

public interface MinecraftHandler {
	public String getJarLocation(AsieLauncher l, String version);
	public void setUpdater(IProgressUpdater updater);
	public boolean download(AsieLauncher l, String version);
	public boolean launch(String path, String username, String sessionID, String jvmArgs, AsieLauncher launcher);
	public boolean isActive();
}
