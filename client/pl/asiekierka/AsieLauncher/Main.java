package pl.asiekierka.AsieLauncher;

public class Main {
	public static void main(String[] args) {
		AsieLauncherGUI al = new AsieLauncherGUI();
		System.out.println("Connecting...");
		if(!al.init()) {
			System.out.println("Error connecting!");
			System.exit(1);
		}
		boolean isRunning = true;
		while(al.isRunning && isRunning) {
			int sleepLength = 1000;
			if(!al.getLaunchedMinecraft()){
				al.repaint();
				sleepLength = 75;
			} else { // Launched Minecraft
				al.setVisible(false);
				if(!al.isActive()) {
					isRunning = false;
				}
			}
			try { Thread.sleep(sleepLength); }
			catch(Exception e) { e.printStackTrace(); }
		}
		System.out.println("Shutting down.");
		System.exit(0);
	}
}
