package pl.asiekierka.AsieLauncher;

import java.awt.Dimension;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.ImageIcon;
import net.minecraft.Launcher;
import org.json.simple.*;
import org.json.simple.parser.*;
import org.smbarbour.mcu.*;

@SuppressWarnings("unused")
public class AsieLauncher implements IProgressUpdater {
	private static final String WINDOW_NAME = "Fluttercraft";
	public static final String URL = "http://asiekierka.humee.pl:8080/";
	private static final String PREFIX = "/.asielauncher/FluttercraftMC/";
	public ArrayList<ModFile> baseFiles;
	protected String directory;
	private String OS;
	private JSONObject file, oldFile;
	private int fullProgress, fullTotal;
	public IProgressUpdater updater;
	public boolean launchedMinecraft = false;
	private MinecraftFrame frame;
	
	public static JSONObject readJSONFile(String filename) {
		try {
			JSONParser tmp = new JSONParser();
			Object o = tmp.parse(new InputStreamReader(new FileInputStream(filename)));
			if(!(o instanceof JSONObject)) return null;
			else return (JSONObject)o;
		} catch(Exception e) { e.printStackTrace(); return null; }
	}
	
	public static JSONObject readJSONNetworkFile(String url) {
		try {
			JSONParser tmp = new JSONParser();
			Object o = tmp.parse(new InputStreamReader(new URL(url).openStream()));
			if(!(o instanceof JSONObject)) return null;
			else return (JSONObject)o;
		} catch(Exception e) { e.printStackTrace(); return null; }
	}
	
	public static boolean saveString(String filename, String data) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write(data);
			writer.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			if(writer != null) try{writer.close();}catch(Exception ee){}
			return false;
		}
		return true;
	}
	
	public static String loadString(String filename) {
		String line = "";
		File file = new File(filename);
		if(!file.exists()) return line;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			line = reader.readLine();
			reader.close();
		}
		catch(Exception e) { e.printStackTrace(); }
		return line;
	}
	
	public ArrayList<ModFile> loadModFiles(JSONArray jsonList, String mode, String prefix) {
		ArrayList<ModFile> list = new ArrayList<ModFile>();
		for(Object o: jsonList) {
			if(o instanceof JSONObject) {
				if(mode.equals("http")) {
					ModFile modFile = new ModFileHTTP(this, (JSONObject)o, prefix);
					list.add(modFile);
				} else if(mode.equals("zip")) {
					ModFile modFile = new ModFileZip(this, (JSONObject)o, "zips/" + prefix);
					list.add(modFile);
				}
			}
		}
		return list;
	}
	
	public ArrayList<ModFile> loadModFiles(JSONArray jsonList, String mode) {
		return loadModFiles(jsonList, mode, "");
	}
	
	public ArrayList<ModFile> loadModFiles(JSONObject object, String mode, String prefix) {
		if(mode.equals("http")) {
			JSONArray array = (JSONArray)object.get("files");
			return loadModFiles(array, mode, prefix);
		} else if(mode.equals("zip")) {
			JSONArray array = (JSONArray)object.get("zips");
			return loadModFiles(array, mode, prefix);
		} else return null;
	}
	
	public ArrayList<ModFile> loadModFiles(JSONObject object, String mode) {
		return loadModFiles(object, mode, "");
	}
	
	public HashMap<String, JSONObject> getOptionMap() {
		HashMap<String, JSONObject> optionMap = new HashMap<String, JSONObject>();
		JSONArray array = (JSONArray)file.get("options");
		for(Object o: array) {
			JSONObject option = (JSONObject)o;
			optionMap.put((String)option.get("id"), option);
		}
		return optionMap;
	}
	public AsieLauncher() {
		directory = System.getProperty("user.home") + PREFIX;
		if(!(new File(directory).exists())) {
			new File(directory).mkdirs();
		} else {
			oldFile = readJSONFile(directory + "also.json");
		}
		System.setProperty("user.dir", (new File(directory).getAbsolutePath()));
		OS = Utils.getSystemName();
		System.out.println("OS: " + OS);
	}
	
	public boolean init() {
		file = readJSONNetworkFile(URL + "also.json");
		return (file instanceof JSONObject);
	}
	
	public boolean save() {
		return saveString(directory + "also.json", file.toJSONString());
	}
	
	public JSONObject getPlatformData(JSONObject source) {
		JSONObject platforms = (JSONObject)source.get("platforms");
		return (JSONObject)platforms.get(OS);
	}

	public JSONObject getOptionData(JSONObject source, String id) {
		JSONObject options = (JSONObject)source.get("options");
		return (JSONObject)options.get(id);
	}
	
	public void update(int progress, int total) {
		if(updater != null) updater.update((fullProgress+progress), fullTotal);
		if(progress == total) fullProgress += total;
	}
	public void setStatus(String status) {
		System.out.println("Status: "+status);
		if(updater != null) updater.setStatus(status);
	}
	
	public ArrayList<ModFile> getFileList(JSONObject source, ArrayList<String> options) {
		ArrayList<ModFile> files = loadModFiles(source, "http");
		files.addAll(loadModFiles(source, "zip"));
		files.addAll(loadModFiles(getPlatformData(source), "http", "platform/"+OS+"/"));
		for(String id: options) {
			JSONObject data = getOptionData(source, id);
			if(data == null) continue;
			boolean doZip = (Boolean)data.get("zip");
			files.addAll(loadModFiles(data, doZip?"zip":"http", "option/"+id+"/"));
		}
		System.out.println("getFileList: got " + files.size() + " files");
		return files;
	}
	
	public int calculateTotalSize(ArrayList<ModFile> files) {
		int totalSize = 0;
		for(ModFile mf: files) {
			totalSize += mf.filesize;
		}
		return totalSize;
	}
	
	public boolean install() {
		ArrayList<String> options = new ArrayList<String>();
		ArrayList<String> oldOptions = new ArrayList<String>();
		return install(options, oldOptions);
	}
	public boolean install(ArrayList<String> options, ArrayList<String> oldOptions) {
		ArrayList<ModFile> installFiles = getFileList(file, options);
		ArrayList<ModFile> oldInstallFiles = null;
		if(oldFile != null) {
			oldInstallFiles = getFileList(oldFile, oldOptions);
			// Delete old files
			for(ModFile mf: oldInstallFiles) {
				if(!installFiles.contains(mf)) {
					this.setStatus(Strings.REMOVING+" "+mf.filename+"...");
					if(!mf.remove()) {
						this.setStatus(Strings.REMOVING+" "+Strings.FAILED);
						return false;
					}
				}
			}
		}
		else oldInstallFiles = new ArrayList<ModFile>();
		fullTotal = calculateTotalSize(installFiles);
		for(ModFile mf: installFiles) {
			this.setStatus(Strings.INSTALLING+" "+mf.filename+"...");
			if(!mf.install(this)) {
				this.setStatus(Strings.INSTALLING+" "+Strings.FAILED);
				return false;
			}
		}
		this.setStatus(Strings.SAVING);
		this.save();
		return true;
	}
	
	public boolean isActive() {
		return !launchedMinecraft || frame.isAppletActive();
	}
	
	public void launch(String username, String sessionID) {
		frame = new MinecraftFrame(WINDOW_NAME, new ImageIcon(this.getClass().getResource("/resources/icon.png")));
		if(updater != null) updater.update(100,100);
		this.setStatus(Strings.LAUNCHING);
		frame.launch(new File(directory),
				new File(directory, "bin"),
				username, sessionID,
				"", new Dimension(854, 480), false);
		try { Thread.sleep(500); } catch(Exception e){}
		launchedMinecraft = true;
	}
}
