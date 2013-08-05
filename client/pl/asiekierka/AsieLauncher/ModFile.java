package pl.asiekierka.AsieLauncher;

import java.io.*;
import org.json.simple.*;

public abstract class ModFile {
	public String filename;
	protected String absoluteFilename;
	protected File file;
	protected AsieLauncher launcher;
	protected JSONObject information;
	public int filesize;
	public boolean overwrite;
	
	public ModFile(AsieLauncher _launcher, JSONObject data, String prefix) {
		launcher = _launcher;
		information = data;
		filename = (String)information.get("filename");
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
