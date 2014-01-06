package pl.asiekierka.AsieLauncher.download;

import java.io.*;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.Utils;

public abstract class FileDownloader {
	private String filename, md5, outputLocation;
	protected File file;
	private int filesize;
	private boolean overwrite;
	
	public FileDownloader(String out, String md5, int filesize, boolean overwrite) {
		this.file = new File(out);
		this.filename = this.file.getName();
		this.filesize = filesize;
		this.outputLocation = out;
		this.md5 = md5;
		this.overwrite = overwrite;
	}

	public String getMD5() {
		return md5;
	}

	public File getFile() {
		return file;
	}

	public String getFilename() {
		return filename;
	}
	
	public String getLocation() {
		return this.outputLocation;
	}
	
	public int getFilesize() {
		return filesize;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public abstract boolean install(IProgressUpdater updater);
	public abstract boolean remove();
	
	public boolean verify(boolean checkMD5) {
		if(file.exists()) {
			try {
				if(checkMD5 && md5 != null && md5.length() > 0) {
					String fileMD5 = Utils.md5(file);
					if(fileMD5.equalsIgnoreCase(md5)) {
						return true;
					} else System.out.println("Incorrect hash!");
				} else {
					// Check filesize instead. Should work on most occasions.
					Long fileFilesizeLong = (Long)(file.length());
					int fileFilesize = fileFilesizeLong.intValue();
					if(this.filesize == fileFilesize) {
						return true;
					} else System.out.println("Incorrect filesize!");
				}
			} catch(Exception e) { /* Can be ignored. */ }
		}
		return false;
	}
	
	public boolean verify() { return verify(true); }
	
	public void createDirsIfMissing() {
		File path = new File(file.getParentFile().getPath());
		path.mkdirs();
	}
	
	// Those serve mostly for removing duplicates.
	@Override
	public boolean equals(Object other) {
		if(other == null || !(other instanceof FileDownloader)) return false;
		FileDownloader fother = (FileDownloader)other;
		return fother.filename.equals(filename);
    }
	
	@Override
	public int hashCode() {
		return filename.hashCode();
	}
}
