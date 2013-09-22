package pl.asiekierka.AsieLauncher.launcher;

public interface IProgressUpdater {
	public void update(int progress, int total);
	public void setStatus(String status);
}
