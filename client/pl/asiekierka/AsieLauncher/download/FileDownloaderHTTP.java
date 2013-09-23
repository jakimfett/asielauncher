package pl.asiekierka.AsieLauncher.download;

import java.net.*;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class FileDownloaderHTTP extends FileDownloader {
	protected URL url;

	public FileDownloaderHTTP(String in, String out, String md5, int filesize, boolean overwrite) {
		super(out, md5, filesize, overwrite);
		try { this.url = new URL(Utils.fixURLString(in)); }
		catch(Exception e) { e.printStackTrace(); this.url = null; }
	}
	
	@Override
	public boolean install(IProgressUpdater updater) {
		return install(updater, this.isOverwrite());
	}
	
	public boolean install(IProgressUpdater updater, boolean overwrite) {
		super.createDirsIfMissing();
		if(verify() || (this.file.exists() && !overwrite)) { // Verified as okay (or already exists), don't download.
			updater.update(this.getFilesize(), this.getFilesize());
			return true;
		}
		// Download
		updater.setStatus(Strings.DOWNLOADING+" "+this.getFilename()+"...");
		boolean downloaded = Utils.download(url, this.getLocation(), this.getFilesize(), updater);
		if(downloaded) {
			return verify(false); // TODO: Fix MD5 checking
		} else return false;
	}
	
	@Override
	public boolean remove() {
		if(this.getFile().exists()) return this.getFile().delete();
		else return true;
	}
}
