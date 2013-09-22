package pl.asiekierka.AsieLauncher.launcher;

import java.net.*;

import org.json.simple.*;

import pl.asiekierka.AsieLauncher.common.Utils;

public class ModFileHTTP extends ModFile {
	protected URL url;

	public ModFileHTTP(AsieLauncher _launcher, JSONObject data, String prefix) {
		super(_launcher, data, prefix);
		String targetURL = launcher.URL + prefix + this.getFilename();
		try { url = new URL(Utils.fixURLString(targetURL)); }
		catch(Exception e) { e.printStackTrace(); url = null; }
	}
	
	@Override
	public boolean install(IProgressUpdater updater) {
		return install(updater, false);
	}
	
	public boolean install(IProgressUpdater updater, boolean forceOverwrite) {
		int filesize = this.getFilesize();
		super.createDirsIfMissing();
		if(!shouldTouch() && !forceOverwrite) {
			updater.update(filesize, filesize);
			return true;
		}
		// Download
		updater.setStatus(Strings.DOWNLOADING+" "+this.getFilename()+"...");
		boolean downloaded = Utils.download(url, absoluteFilename, filesize, updater);
    	/*
    	if(downloaded) {
			try {
				String fileMD5 = Utils.md5(file);
				if(!fileMD5.equalsIgnoreCase(md5)) return false;
				else return true;
			} catch(Exception e) { /* Can be ignored. */ /* }
    	} else return false; */
    	return downloaded;
	}
	
	@Override
	public boolean remove() {
		if(this.getFile().exists()) return this.getFile().delete();
		else return true;
	}
}
