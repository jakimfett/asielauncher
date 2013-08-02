package pl.asiekierka.AsieLauncher;

import java.io.*;
import org.json.simple.*;

public class ModFileZip extends ModFileHTTP {
	public String directory;
	
	public ModFileZip(AsieLauncher _launcher, JSONObject data, String prefix) {
		super(_launcher, data, prefix);
		absoluteFilename = launcher.directory + prefix + (String)information.get("filename");
		file = new File(absoluteFilename);
		directory = launcher.directory + (String)data.get("directory");
	}
	@Override
	public boolean install(IProgressUpdater updater) {
		boolean downloaded = super.install(updater);
    	if(!downloaded) return false;
    	File dirFile = new File(directory);
    	dirFile.mkdir();
		updater.setStatus(Strings.UNPACKING+" "+filename+"...");
    	try {
    		Utils.extract(absoluteFilename, directory);
    	} catch(Exception e) { e.printStackTrace(); return false; }
    	return true;
	}

	@Override
	public boolean remove() {
		return true; // DUMMY! We can't really figure out what we removed, besides, users might have touched it.
	}
}
