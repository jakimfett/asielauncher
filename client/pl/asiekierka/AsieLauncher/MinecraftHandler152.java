package pl.asiekierka.AsieLauncher;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;

import org.smbarbour.mcu.MinecraftFrame;

public class MinecraftHandler152 implements MinecraftHandler {
	private MinecraftFrame frame;
	public MinecraftHandler152() {
		// TODO Auto-generated constructor stub
	}
	
	public String getJarLocation(AsieLauncher l, String version) {
		File dir = new File(l.baseDir + "versions/" + version + "/");
		if(!dir.exists()) dir.mkdirs();
		return l.baseDir + "versions/" + version + "/minecraft.jar";
	}
	@Override
	public boolean download(AsieLauncher l, String version) {
		boolean downloaded = false;
		if(!(new File(getJarLocation(l, version)).exists())) {
			// Does not exist, download.
			try {
				URL url = new URL("http://assets.minecraft.net/"+version.replace('.', '_')+"/minecraft.jar");
				downloaded = Utils.download(url, getJarLocation(l, version));
			} catch(Exception e) { e.printStackTrace(); }
		}
		return downloaded;
	}
	
	@Override
	public boolean isActive() {
		return (frame != null && frame.isAppletActive());
	}
	private void setStatus(AsieLauncher l, String status) {
		if(l.updater != null) l.updater.setStatus(status);
	}
	
	public ArrayList<String> getMCArguments(AsieLauncher l, String path, String jarPath, String classpath, String username, String sessionID, String jvmArgs) {
		ArrayList<String> args = new ArrayList<String>();
		args.add(jarPath);
		args.addAll(Arrays.asList(jvmArgs.split(" ")));
		args.add("-cp"); args.add(classpath);
		args.add("org.smbarbour.mcu.MinecraftFrame");
		args.add(username); args.add(sessionID); args.add(l.WINDOW_NAME);
		args.add(new File(path).getAbsolutePath());
		args.add(new File(path, "bin").getAbsolutePath());
		args.add("854"); args.add("480");
		args.add("null"); args.add("false");
		System.out.println("Launching with arguments: " + args.toString());
		return args;
	}
	
	@Override
	public boolean launch(String path, String username, String sessionID, String jvmArgs, AsieLauncher l) {
		if(sessionID.length() == 0) sessionID = "null";
		// Launch Minecraft.
		String separator = System.getProperty("file.separator");
	    String classpath = System.getProperty("java.class.path");
	    System.out.println(l.getLoadDir());
	    System.out.println(classpath);
	    if(classpath.indexOf(separator) == -1 || (l.getLoadDir().indexOf("/") == 0 && classpath.indexOf("/") != 0)) {
	    	classpath = (new File(l.getLoadDir(), classpath)).getAbsolutePath();
	    }
	    System.out.println(classpath);
	    String jarPath = System.getProperty("java.home")
	            + separator + "bin" + separator + Utils.getJavaBinaryName();
		setStatus(l, Strings.LAUNCHING);
	    if((new File(jarPath)).exists()) {
	    	System.out.println("Launching via process spawner");
	    	ProcessBuilder processBuilder = new ProcessBuilder(getMCArguments(l,path,jarPath,classpath,username,sessionID,jvmArgs));
	    	try {
	    		processBuilder.directory(new File(path));
	    		Process process = processBuilder.start();
		    	Utils.pipeOutput(process);
	    		try { Thread.sleep(500); } catch(Exception e){}
	    		return true;
	    	}
	    	catch(Exception e) { }
	    }
	    // Failsafe
	    System.out.println("Launching via internal Java process");
	    System.setProperty("user.dir", (new File(path).getAbsolutePath()));
	    frame = new MinecraftFrame(l.WINDOW_NAME, new ImageIcon(this.getClass().getResource("/resources/icon.png")));
		frame.launch(new File(path),
				new File(path, "bin"),
				username, sessionID,
				"", new Dimension(854, 480), false);
		try { Thread.sleep(500); } catch(Exception e){}
		return true;
	}

}
