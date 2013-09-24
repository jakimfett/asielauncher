package pl.asiekierka.AsieLauncher.launcher;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import javax.swing.ImageIcon;

import org.smbarbour.mcu.MinecraftFrame;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.JavaLauncher;
import pl.asiekierka.AsieLauncher.common.Utils;

public class MinecraftHandler152 implements MinecraftHandler {
	private MinecraftFrame frame;
	public MinecraftHandler152() {
		// TODO Auto-generated constructor stub
	}
	
	public String getJarLocation(AsieLauncher l, String version) {
		File dir = new File(l.baseDir + "versions/" + version + "/");
		if(!dir.exists()) dir.mkdirs();
		return Utils.getPath(l.baseDir + "versions/" + version + "/minecraft.jar");
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
	
	public ArrayList<String> getMCArguments(AsieLauncher l, String path, String classpath, String username, String sessionID, String jvmArgs) {
		ArrayList<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(jvmArgs.split(" ")));
		args.add("-cp"); args.add(classpath);
		args.add("org.smbarbour.mcu.MinecraftFrame");
		args.add(username); args.add(sessionID); args.add(l.WINDOW_NAME);
		args.add(new File(path).getAbsolutePath());
		args.add(new File(path, "bin").getAbsolutePath());
		args.add("854"); args.add("480");
		args.add("null"); args.add("false");
		Utils.logger.log(Level.FINE, "Launching with arguments: " + args.toString());
		return args;
	}
	
	// Dummy
	@Override
	public void setUpdater(IProgressUpdater updater) { }
	
	@Override
	public boolean launch(String path, String username, String sessionID, String jvmArgs, AsieLauncher l) {
		if(sessionID.length() == 0) sessionID = "null";
		// Launch Minecraft.
		String separator = System.getProperty("file.separator");
	    String classpath = System.getProperty("java.class.path");
	    if(classpath.indexOf(separator) == -1 || (l.getLoadDir().indexOf("/") == 0 && classpath.indexOf("/") != 0)) {
	    	classpath = (new File(Utils.getPath(l.getLoadDir()), classpath)).getAbsolutePath();
	    }
		setStatus(l, Strings.LAUNCHING);
	    if(!JavaLauncher.launch(path, getMCArguments(l,path,classpath,username,sessionID,jvmArgs))) {
		    // Failsafe
		    Utils.logger.log(Level.INFO, "Launching via internal Java process!!!");
		    System.setProperty("user.dir", (new File(path).getAbsolutePath()));
		    frame = new MinecraftFrame(l.WINDOW_NAME, new ImageIcon(this.getClass().getResource("/resources/icon.png")));
			frame.launch(new File(path),
					new File(path, "bin"),
					username, sessionID,
					"", new Dimension(854, 480), false);
			try { Thread.sleep(500); } catch(Exception e){}
	    }
	    return true;
	}

}
