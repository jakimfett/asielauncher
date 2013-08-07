package pl.asiekierka.AsieLauncher;

public interface MinecraftHandler {
	public String getJarLocation(AsieLauncher l, String version);
	public boolean download(AsieLauncher l, String version);
	public boolean launch(String path, String username, String password, boolean onlineMode, String jvmArgs, AsieLauncher launcher);
	public boolean isActive();
}
