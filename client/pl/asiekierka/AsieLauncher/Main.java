package pl.asiekierka.AsieLauncher;

public class Main {
	public static void main(String[] args) {
		AsieLauncherGUI al = new AsieLauncherGUI();
		System.out.println("Connecting...");
		if(!al.init()) {
			System.out.println("Error connecting!");
			System.exit(1);
		}
		while(al.isRunning) {
			al.repaint();
			try { Thread.sleep(50); }
			catch(Exception e) { e.printStackTrace(); }
		}
		System.out.println("Shutting down.");
		System.exit(0);
		//al.install();
	}
}
