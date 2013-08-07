package pl.asiekierka.AsieLauncher;

import java.io.*;
import java.net.*;
import org.json.simple.*;

public class ModFileHTTP extends ModFile {
	protected URL url;

	public ModFileHTTP(AsieLauncher _launcher, JSONObject data, String prefix) {
		super(_launcher, data, prefix);

		try { url = new URL(Utils.fixURLString(launcher.URL + prefix + filename)); }
		catch(Exception e) { e.printStackTrace(); url = null; }
	}
	@Override
	public boolean install(IProgressUpdater updater) {
		return install(updater, false);
	}
	
	public boolean install(IProgressUpdater updater, boolean forceOverwrite) {
		super.createDirsIfMissing();
		if(!shouldTouch() && !forceOverwrite) {
			updater.update(filesize, filesize);
			return true;
		}
		// Download
		updater.setStatus(Strings.DOWNLOADING+" "+filename+"...");
		boolean downloaded = true;
    	BufferedInputStream in = null;
    	FileOutputStream out = null;
    	try {
    		int count;
    		int totalCount = 0;
    		byte data[] = new byte[1024];
    		in = new BufferedInputStream(url.openStream());
    		out = new FileOutputStream(absoluteFilename);
    		while ((count = in.read(data, 0, 1024)) != -1) {
    			out.write(data, 0, count);
    			totalCount += count;
    			updater.update(totalCount, filesize);
    		}
    	}
    	catch(Exception e) { e.printStackTrace(); System.out.println("Download error!"); downloaded = false; }
    	finally {
    		try {
    			if (in != null) in.close();
    			if (out != null) out.close();
    		} catch(Exception e) { e.printStackTrace(); }
    	}
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
		if(file.exists()) return file.delete();
		else return true;
	}
}
