package pl.asiekierka.AsieLauncher.common;

public interface IProgressUpdater {
	public void update(int progress, int total);
	public void setStatus(String status);
}
