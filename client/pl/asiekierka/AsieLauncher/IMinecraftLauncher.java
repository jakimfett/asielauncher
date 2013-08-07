package pl.asiekierka.AsieLauncher;

public interface IMinecraftLauncher {
	public boolean launch(String path, String username, String password, boolean onlineMode, String jvmArgs, AsieLauncher launcher);
	public boolean isActive();
}
