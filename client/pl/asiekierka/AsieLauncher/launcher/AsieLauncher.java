package pl.asiekierka.AsieLauncher.launcher;

import java.awt.Dimension;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.ImageIcon;

import net.minecraft.Launcher;

import org.json.simple.*;
import org.json.simple.parser.*;
import org.smbarbour.mcu.*;

import pl.asiekierka.AsieLauncher.auth.Authentication;
import pl.asiekierka.AsieLauncher.auth.AuthenticationMojangLegacy;
import pl.asiekierka.AsieLauncher.auth.AuthenticationYggdrasil;
import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.download.FileDownloader;
import pl.asiekierka.AsieLauncher.download.FileDownloaderHTTP;
import pl.asiekierka.AsieLauncher.download.FileDownloaderZip;

@SuppressWarnings("unused")
public class AsieLauncher implements IProgressUpdater {
	public static final int VERSION = 6;
	private ServerListManager serverlist;
	public static final String VERSION_STRING = "0.4.1-dev";
	public String WINDOW_NAME = "AsieLauncher";
	public String URL = "http://127.0.0.1:8080/";
	private String PREFIX = "default";
	private ArrayList<FileDownloader> baseFiles;
	public String directory, baseDir;
	private String OS;
	private JSONObject file, oldFile, configFile;
	private int fullProgress, fullTotal;
	protected IProgressUpdater updater;
	private boolean launchedMinecraft = false;
	private String loadDir;
	private MinecraftHandler mc;
	private Authentication auth;
	public String mcVersion;
	public String defaultJvmArgs;
	public static Logger logger;
	
	static {
		logger = Logger.getLogger(AsieLauncher.class.getName());
	}
	
	public boolean canKeepPassword() {
		return (auth instanceof AuthenticationYggdrasil);
	}
	
	public void setKeepPassword(boolean l) {
		if(auth instanceof AuthenticationYggdrasil) {
			AuthenticationYggdrasil auth = (AuthenticationYggdrasil)this.auth;
			auth.setKeepPassword(l);
		}
	}
	
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
	
	public boolean compatibleClientRevision() {
		if(file == null) return true; // Assumptions!
		Long revNew = (Long)(file.get("client_revision"));
		if(revNew == null) { return false; // Ancient
		} else return revNew.intValue() == VERSION;
	}
	
	public ArrayList<FileDownloader> loadModFiles(JSONArray jsonList, String mode, String prefix) {
		ArrayList<FileDownloader> list = new ArrayList<FileDownloader>();
		for(Object o: jsonList) {
			if(o instanceof JSONObject) {
				JSONObject mod = (JSONObject) o;
				String filename = (String)mod.get("filename");
				Long filesize = (Long)mod.get("size");
				String md5 = (String)mod.get("md5");
				boolean overwrite = (Boolean)mod.get("overwrite");
				if(mode.equals("http")) {
					String inputAddress = URL + prefix + filename;
					String outputFile = directory + filename;
					FileDownloader fileDownloader = new FileDownloaderHTTP(inputAddress, outputFile, md5, filesize.intValue(), overwrite);
					list.add(fileDownloader);
				} else if(mode.equals("zip")) {
					String inputAddress = URL + "zips/" + prefix + filename;
					String outputFile = directory + "zips/" + filename;
					String unpackLocation = directory + (String)mod.get("directory") + "/";
					FileDownloader fileDownloader = new FileDownloaderZip(inputAddress, outputFile, unpackLocation, md5, filesize.intValue(), overwrite);
					list.add(fileDownloader);
				}
			}
		}
		return list;
	}
	
	public ArrayList<FileDownloader> loadModFiles(JSONArray jsonList, String mode) {
		return loadModFiles(jsonList, mode, "");
	}
	
	public ArrayList<FileDownloader> loadModFiles(JSONObject object, String mode, String prefix) {
		if(object == null) return new ArrayList<FileDownloader>();
		if(mode.equals("http") && object.containsKey("files")) {
			JSONArray array = (JSONArray)object.get("files");
			return loadModFiles(array, mode, prefix);
		} else if(mode.equals("zip") && object.containsKey("zips")) {
			JSONArray array = (JSONArray)object.get("zips");
			return loadModFiles(array, mode, prefix);
		} else return new ArrayList<FileDownloader>();
	}
	
	public ArrayList<FileDownloader> loadModFiles(JSONObject object, String mode) {
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
		if(!URL.startsWith("http://") && (URL.indexOf("://") < 0)) {
			URL = "http://" + URL;
		}
	}
	
	public AsieLauncher() {
		configureConfig();
		baseDir = System.getProperty("user.home") + "/.asielauncher/";
		directory = baseDir + PREFIX + "/";
		logger.addHandler(new ConsoleHandler());
		try {
			FileHandler handler = new FileHandler(Utils.getPath(directory + "AsieLauncher.log"), false);
			handler.setFormatter(new SimpleFormatter());
			logger.addHandler(handler);
		} catch(Exception e) { e.printStackTrace(); }
		serverlist = new ServerListManager(directory + "servers.dat", false);
		if(!(new File(directory).exists())) {
			new File(directory).mkdirs();
		} else {
			oldFile = Utils.readJSONFile(directory + "also.json");
		}
		loadDir = (new File(".").getAbsolutePath());
		loadDir = loadDir.substring(0,loadDir.length()-1);
		OS = Utils.getSystemName();
		logger.log(Level.INFO, "OS: " + OS);
	}
	
	public boolean isSupported() {
		if(this.mcVersion == null) return true; // Default hack
		return Utils.versionToInt(this.mcVersion) <= Utils.versionToInt("1.6.4");
	}
	public boolean init() {
		file = Utils.readJSONUrlFile(URL + "also.json");
		if(!(file instanceof JSONObject)) {
			file = oldFile;
		}
		if(file instanceof JSONObject) { // Set variables;.
			Object o = file.get("onlineMode");
			boolean onlineMode = false;
			if(o instanceof Boolean) onlineMode = ((Boolean)o);
			if(getFileRevision(file) >= 5) {
				mcVersion = (String)file.get("mcVersion");
				if(getFileRevision(file) >= 6) {
					WINDOW_NAME = (String)file.get("windowName");
				} else WINDOW_NAME = "";
			} else mcVersion = "1.6.2";
			if(Utils.versionToInt(this.mcVersion) <= Utils.versionToInt("1.5.2")) {
				mc = new MinecraftHandler152();
				if(onlineMode) auth = new AuthenticationMojangLegacy();
			} else {
				mc = new MinecraftHandler162();
				if(onlineMode) auth = new AuthenticationYggdrasil(directory, false);
			}
			if(file.containsKey("jvmArguments")) {
				defaultJvmArgs = (String)file.get("jvmArguments");
			}
			return (file != oldFile);
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
		logger.log(Level.INFO, "Status: "+status);
		if(updater != null) updater.setStatus(status);
	}
	
	public ArrayList<FileDownloader> getFileList(JSONObject source, ArrayList<String> options) {
		ArrayList<FileDownloader> files = loadModFiles(source, "http");
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
		logger.log(Level.FINER, "getFileList: got " + files.size() + " files");
		return files;
	}
	
	public int calculateTotalSize(ArrayList<FileDownloader> files) {
		int totalSize = 0;
		for(FileDownloader mf: files) {
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
		ArrayList<FileDownloader> installFiles = getFileList(file, options);
		ArrayList<FileDownloader> oldInstallFiles = null;
		int dryTotal = 0;
		installLog = new ArrayList<String>();
		if(oldFile != null) {
			oldInstallFiles = getFileList(oldFile, oldOptions);
			// Delete old files
			for(FileDownloader mf: oldInstallFiles) {
				if(!installFiles.contains(mf)) {
					if(dry && !(mf instanceof FileDownloaderZip)) { // Dry run
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
		else oldInstallFiles = new ArrayList<FileDownloader>();
		fullTotal = calculateTotalSize(installFiles);
		for(FileDownloader mf: installFiles) {
			if(dry && !mf.verify()) {
				dryTotal += mf.getFilesize();
				if(mf instanceof FileDownloaderZip) {
					FileDownloaderZip mfz = (FileDownloaderZip) mf;
					installLog.add("[+] " + mfz.getFilename() + " => ./" + mfz.unpackLocation);
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
				sessionID = auth.getSessionToken();
			}
		}
		launchedMinecraft = mc.launch(directory, username, sessionID, jvmArgs, this);
	}
}
