package pl.asiekierka.AsieLauncher.download;

import java.io.*;

import org.json.simple.*;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class FileDownloaderZip extends FileDownloaderHTTP {
	public String directory, installDirectory;
	
	public FileDownloaderZip(String directory, String url, JSONObject data, String prefix) {
		super(directory, url, data, prefix);
		absoluteFilename = directory + prefix + (String)information.get("filename");
		file = new File(absoluteFilename);
		installDirectory = (String)data.get("directory");
		this.directory = directory + installDirectory;
	}
	@Override
	public boolean install(IProgressUpdater updater) {
		boolean downloaded = super.install(updater, true);
    	if(!downloaded) return false;
    	File dirFile = new File(directory);
    	dirFile.mkdir();
		updater.setStatus(Strings.UNPACKING+" "+this.getFilename()+"...");
    	try {
    		Utils.extract(absoluteFilename, directory, this.isOverwrite());
    	} catch(Exception e) { e.printStackTrace(); return false; }
    	return true;
	}

	@Override
	public boolean remove() {
		return true; // DUMMY! We can't really figure out what we removed, besides, users might have touched it.
	}
}
