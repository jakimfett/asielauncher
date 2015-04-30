package pl.asiekierka.AsieLauncher.launcher;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;

public interface MinecraftHandler {
	public String getJarLocation(AsieLauncher l);
	public void setUpdater(IProgressUpdater updater);
	public boolean download(AsieLauncher l);
	public boolean launch(String path, String username, String sessionID, String UUID, String jvmArgs, AsieLauncher launcher);
	public boolean isActive();
	//public ArrayList<FileDownloader> getFiles(AsieLauncher l);
}
