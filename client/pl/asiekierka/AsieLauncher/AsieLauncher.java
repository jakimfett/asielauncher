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
	public static final int VERSION = 5;
	private ServerListHandler serverlist;
	public static final String VERSION_STRING = "0.4.0-dev";
	public String WINDOW_NAME = "AsieLauncher";
	public String URL = "http://127.0.0.1:8080/";
	private String PREFIX = "/.asielauncher/default/";
	private ArrayList<ModFile> baseFiles;
	protected String directory, baseDir;
	private String OS;
	private JSONObject file, oldFile, configFile;
	private int fullProgress, fullTotal;
	protected IProgressUpdater updater;
	private boolean launchedMinecraft = false;
	private String loadDir;
	private MinecraftHandler mc;
	private Authentication auth;
	public String mcVersion;
	
	public String getLoadDir() {
		return loadDir;
	}

	public IProgressUpdater getUpdater() {
		return updater;
	}

	public void setUpdater(IProgressUpdater updater) {
		this.updater = updater;
	}

	public boolean isOnlineMode() { return (auth != null); }
	
	public int getFileRevision(JSONObject source) {
		Long revNew = (Long)(source.get("client_revision"));
		if(revNew == null) return 1;
		return revNew.intValue();
	}
	
	public boolean sameClientRevision() {
		if(file == null) return true; // Assumptions!
		Long revNew = (Long)(file.get("client_revision"));
		if(revNew == null) {
			return (VERSION == 1);
		} else return revNew.intValue() == VERSION;
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
		if(object == null) return new ArrayList<ModFile>();
		if(mode.equals("http") && object.containsKey("files")) {
			JSONArray array = (JSONArray)object.get("files");
			return loadModFiles(array, mode, prefix);
		} else if(mode.equals("zip") && object.containsKey("zips")) {
			JSONArray array = (JSONArray)object.get("zips");
			return loadModFiles(array, mode, prefix);
		} else return new ArrayList<ModFile>();
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
		configFile = Utils.readJSONUrlFile(getClass().getResource("/resources/config.json"));
		PREFIX = (String)configFile.get("directoryName");
		URL = (String)configFile.get("serverUrl");
		WINDOW_NAME = (String)configFile.get("windowName");
	}
	public AsieLauncher() {
		configureConfig();
		baseDir = System.getProperty("user.home") + "/.asielauncher/";
		directory = baseDir + PREFIX + "/";
		serverlist = new ServerListHandler(directory + "servers.dat", false);
		if(!(new File(directory).exists())) {
			new File(directory).mkdirs();
		} else {
			oldFile = Utils.readJSONFile(directory + "also.json");
		}
		loadDir = (new File(".").getAbsolutePath());
		loadDir = loadDir.substring(0,loadDir.length()-1);
		OS = Utils.getSystemName();
		System.out.println("OS: " + OS);
	}
	
	public boolean isSupported() {
		if(this.mcVersion == null) return true; // Default hack
		return Utils.versionToInt(this.mcVersion) <= Utils.versionToInt("1.6.2");
	}
	public boolean init() {
		file = Utils.readJSONUrlFile(URL + "also.json");
		if(file instanceof JSONObject) { // Set variables;.
			Object o = file.get("onlineMode");
			boolean onlineMode = false;
			if(o instanceof Boolean) onlineMode = ((Boolean)o);
			if(getFileRevision(file) >= 5) {
				mcVersion = (String)file.get("mcVersion");
			} else mcVersion = "1.6.2";
			if(Utils.versionToInt(this.mcVersion) <= Utils.versionToInt("1.5.2")) {
				mc = new MinecraftHandler152();
				if(onlineMode) auth = new AuthenticationMojangLegacy();
			} else {
				mc = new MinecraftHandler162();
				if(onlineMode) auth = new AuthenticationYggdrasil(directory, false);
			}
			return true;
		}
		return false;
	}
	
	public boolean askForPassword() {
		return (auth != null && auth.requiresPassword());
	}
	
	public boolean save() {
		return Utils.saveStringToFile(directory + "also.json", file.toJSONString());
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
		if(mc instanceof MinecraftHandler152) {
			// 1.5.2 still downloads platform data.
			files.addAll(loadModFiles(getPlatformData(source), "http", "platform/"+OS+"/"));
		}
		if(getFileRevision(source) >= 2) for(String id: options) {
			JSONObject data = getOptionData(source, id);
			if(data == null) continue;
			boolean doZip = (Boolean)data.get("zip");
			files.addAll(loadModFiles(data, doZip?"zip":"http", "options/"+id+"/"));
		}
		if(getFileRevision(source) >= 5) {
			JSONArray data = (JSONArray)source.get("jarPatches");
			files.addAll(loadModFiles(data, "http"));
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
	
	public List<String> getRepackedFiles() {
		ArrayList<String> rf = new ArrayList<String>();
		JSONArray data = (JSONArray)file.get("jarPatches");
		for(Object o: data) {
			if(!(o instanceof JSONObject)) continue;
			JSONObject jo = (JSONObject)o;
			rf.add(directory + (String)jo.get("filename"));
		}
		return rf;
	}
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
		this.setStatus(Strings.DOWNLOAD_MC);
		mc.setUpdater(updater);
		mc.download(this, this.mcVersion);
		if(mc instanceof MinecraftHandler152) {
			Repacker repacker = new Repacker(directory + "bin/minecraft.jar");
			List<String> repackFiles = getRepackedFiles();
			this.setStatus(Strings.REPACK_JAR);
			if(!repacker.repackJar(mc.getJarLocation(this, this.mcVersion), repackFiles.toArray(new String[repackFiles.size()]))) return false;
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
	
	public void launch(String _username, String password, String jvmArgs) {
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
		// Authenticate, if necessary.
		String username = _username;
		String sessionID = "null";
		if(updater != null) updater.update(100,100);
		if(auth != null) {
			setStatus(Strings.AUTH_STATUS);
			if(!auth.authenticate(username, password)) {
				setStatus(Strings.LOGIN_ERROR+": " + auth.getErrorMessage());
				return;
			} else {
				username = auth.getUsername();
				sessionID = auth.getSessionID();
			}
		}
		launchedMinecraft = mc.launch(directory, username, sessionID, jvmArgs, this);
	}
}
