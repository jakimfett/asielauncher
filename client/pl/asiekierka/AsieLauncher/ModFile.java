package pl.asiekierka.AsieLauncher;

import java.io.*;

import org.json.simple.*;

public abstract class ModFile {
	public String filename;
	protected String absoluteFilename, md5;
	protected File file;
	protected AsieLauncher launcher;
	protected JSONObject information;
	public int filesize;
	public boolean overwrite;
	
	public ModFile(AsieLauncher _launcher, JSONObject data, String prefix) {
		launcher = _launcher;
		information = data;
		filename = (String)information.get("filename");
		md5 = (String)data.get("md5");
		Long longFilesize = (Long)(information.get("size"));
		filesize = longFilesize.intValue();
		absoluteFilename = launcher.directory + (String)information.get("filename");
		file = new File(absoluteFilename);
		overwrite = information.containsKey("overwrite")
					? (Boolean)(information.get("overwrite"))
					: true;
	}
	
	public abstract boolean install(IProgressUpdater updater);
	public abstract boolean remove();
	
	public boolean shouldTouch() {
		if(file.exists()) {
			if(!overwrite) return false;
			try {
				String fileMD5 = Utils.md5(file);
				System.out.println("Comparison: "+md5+" "+fileMD5);
				if(fileMD5.equalsIgnoreCase(md5)) {
					return false;
				}
			} catch(Exception e) { /* Can be ignored. */ }
		}
		return true;
	}
	public void createDirsIfMissing() {
		File path = new File(file.getParentFile().getPath());
		path.mkdirs();
	}
	
	@Override
	public boolean equals(Object other) {
		if(other == null || !(other instanceof ModFile)) return false;
		ModFile mother = (ModFile)other;
		return mother.filename.equals(filename);
    }
	
	@Override
	public int hashCode() {
		return filename.hashCode();
	}
}
