package pl.asiekierka.AsieLauncher.common;

import java.util.List;
import java.io.File;

public class JavaLauncher {

	public JavaLauncher() {
		// TODO Auto-generated constructor stub
	}
	
	public static boolean launch(String path, String jarPath, List<String> arguments) {
		arguments.add(0, jarPath);
		if((new File(jarPath)).exists()) {
			ProcessBuilder processBuilder = new ProcessBuilder(arguments);
			try {
				processBuilder.directory(new File(path));
				Process process = processBuilder.start();
				Utils.pipeOutput(process);
				try { Thread.sleep(500); } catch(Exception e){}
				return true;
			}
			catch(Exception e) { return false; }
		} else return false;
    }

}
