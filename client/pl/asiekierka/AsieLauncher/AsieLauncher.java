package pl.asiekierka.AsieLauncher;

import java.awt.Dimension;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import net.minecraft.Launcher;

import org.json.simple.*;
import org.json.simple.parser.*;
import org.smbarbour.mcu.*;

@SuppressWarnings("unused")
public class AsieLauncher implements IProgressUpdater {
	public static final int VERSION = 4;
	private ServerListHandler serverlist;
	public static final String VERSION_STRING = "0.3.0";
	public String WINDOW_NAME = "AsieLauncher";
	public String URL = "http://127.0.0.1:8080/";
	private String PREFIX = "/.asielauncher/default/";
	private ArrayList<ModFile> baseFiles;
	protected String directory;
	private String OS;
	private JSONObject file, oldFile, configFile;
	private int fullProgress, fullTotal;
	protected IProgressUpdater updater;
	private boolean launchedMinecraft = false;
	private String loadDir;
	private boolean onlineMode = true;
	private IMinecraftLauncher mc;
	
	public String getLoadDir() {
		return loadDir;
	}

	public IProgressUpdater getUpdater() {
		return updater;
	}

	public void setUpdater(IProgressUpdater updater) {
		this.updater = updater;
	}

	public boolean isOnlineMode() { return onlineMode; }
	
	public void setOnlineMode(boolean onlineMode) {
		this.onlineMode = onlineMode;
	}

	public int getFileRevision(JSONObject source) {
		Long revNew = (Long)(source.get("client_revision"));
		if(revNew == null) return 1;
		return revNew.intValue();
	}
	
	public boolean sameClientRevision() {
		if(file == null) return true; // Assumptions!
		Long revNew = (Long)(file.get("client_revision"));
		if(revNew == null && VERSION != 1) return false;
		if(revNew == null && VERSION == 1) return true;
		return revNew.intValue() == VERSION;
	}
	
	public static JSONObject readJSONFile(String filename) {
		try {
			JSONParser tmp = new JSONParser();
			Object o = tmp.parse(new InputStreamReader(new FileInputStream(filename)));
			if(!(o instanceof JSONObject)) return null;
			else return (JSONObject)o;
		} catch(Exception e) { e.printStackTrace(); return null; }
	}
	
	public static JSONObject readJSONUrlFile(URL url) {
		try {
			JSONParser tmp = new JSONParser();
			Object o = tmp.parse(new InputStreamReader(url.openStream()));
			if(!(o instanceof JSONObject)) return null;
			else return (JSONObject)o;
		} catch(Exception e) { e.printStackTrace(); return null; }
	}
	
	public static JSONObject readJSONUrlFile(String url) {
		try {
			return readJSONUrlFile(new URL(url));
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
		if(file != null) {
			JSONObject options = (JSONObject)file.get("options");
			for(Object o: options.values()) {
				JSONObject option = (JSONObject)o;
				optionMap.put((String)option.get("id"), option);
			}
		}
		return optionMap;
	}
	public void configureConfig() {
		configFile = readJSONUrlFile(getClass().getResource("/resources/config.json"));
		PREFIX = "/.asielauncher/"+(String)configFile.get("directoryName")+"/";
		URL = (String)configFile.get("serverUrl");
		WINDOW_NAME = (String)configFile.get("windowName");
	}
	public AsieLauncher() {
		configureConfig();
		directory = System.getProperty("user.home") + PREFIX;
		serverlist = new ServerListHandler(directory + "servers.dat", false);
		if(!(new File(directory).exists())) {
			new File(directory).mkdirs();
		} else {
			oldFile = readJSONFile(directory + "also.json");
		}
		loadDir = (new File(".").getAbsolutePath());
		loadDir = loadDir.substring(0,loadDir.length()-1);
		OS = Utils.getSystemName();
		System.out.println("OS: " + OS);
	}
	
	public boolean init() {
		file = readJSONUrlFile(URL + "also.json");
		if(file instanceof JSONObject) { // Set variables;.
			Object o = file.get("onlineMode");
			if(o instanceof Boolean) onlineMode = ((Boolean)o);
			return true;
		}
		return false;
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
		if(getFileRevision(source) >= 2) for(String id: options) {
			JSONObject data = getOptionData(source, id);
			if(data == null) continue;
			boolean doZip = (Boolean)data.get("zip");
			files.addAll(loadModFiles(data, doZip?"zip":"http", "options/"+id+"/"));
		}
		System.out.println("getFileList: got " + files.size() + " files");
		return files;
	}
	
	public int calculateTotalSize(ArrayList<ModFile> files) {
		int totalSize = 0;
		for(ModFile mf: files) {
			totalSize += mf.getFilesize();
		}
		return totalSize;
	}
	
	private ArrayList<String> installLog;
	
	public String[] getInstallLog() { return installLog.toArray(new String[installLog.size()]); }
	
	public boolean install(boolean dry) {
		ArrayList<String> options = new ArrayList<String>();
		ArrayList<String> oldOptions = new ArrayList<String>();
		return install(options, oldOptions, dry);
	}
	public boolean install(ArrayList<String> options, ArrayList<String> oldOptions, boolean dry) {
		ArrayList<ModFile> installFiles = getFileList(file, options);
		ArrayList<ModFile> oldInstallFiles = null;
		int dryTotal = 0;
		installLog = new ArrayList<String>();
		if(oldFile != null) {
			oldInstallFiles = getFileList(oldFile, oldOptions);
			// Delete old files
			for(ModFile mf: oldInstallFiles) {
				if(!installFiles.contains(mf)) {
					if(dry && !(mf instanceof ModFileZip)) { // Dry run
						installLog.add("[-] " + mf.getFilename());
						dryTotal -= mf.getFilesize();
						continue;
					}
					this.setStatus(Strings.REMOVING+" "+mf.getFilename()+"...");
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
			if(dry && mf.shouldTouch()) {
				dryTotal += mf.getFilesize();
				if(mf instanceof ModFileZip) {
					ModFileZip mfz = (ModFileZip) mf;
					installLog.add("[+] " + mfz.getFilename() + " => ./" + mfz.installDirectory);
				} else {
					installLog.add("[+] " + mf.getFilename());
				}
				continue;
			} else if(dry) continue;
			this.setStatus(Strings.INSTALLING+" "+mf.getFilename()+"...");
			if(!mf.install(this)) {
				this.setStatus(Strings.INSTALLING+" "+Strings.FAILED);
				return false;
			}
		}
		if(!dry) {
			this.setStatus(Strings.SAVING);
			this.save();
		} else {
			installLog.add("Total installation size: " + Math.round(dryTotal/1024) + "KB");
		}
		return true;
	}
	
	public boolean isActive() {
		return !launchedMinecraft || (mc != null && mc.isActive());
	}
	
	public void launch(String username, String password, String jvmArgs) {
		// Update serverlist.
		if(file != null) {
			if(updater != null) updater.setStatus(Strings.UPDATE_SERVERLIST);
			HashMap<String, String> servers = new HashMap<String, String>();
			JSONObject serversJson = (JSONObject)file.get("servers");
			for(Object o: serversJson.entrySet()) {
				if(!(o instanceof Entry)) continue;
				@SuppressWarnings("unchecked")
				Entry<? extends Object, ? extends Object> e = (Entry<? extends Object, ? extends Object>)o;
				if(!(e.getKey() instanceof String)) continue;
				if(!(e.getValue() instanceof String)) continue;
				servers.put((String)e.getKey(), (String)e.getValue());
			}
			serverlist.updateServerList(servers);
		}
		mc = new MinecraftLauncher152();
		launchedMinecraft = mc.launch(directory, username, password, onlineMode, jvmArgs, this);
	}
}
