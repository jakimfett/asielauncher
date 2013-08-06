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
	public static final int VERSION = 3;
	public static final String VERSION_STRING = "0.2.4-dev";
	public String WINDOW_NAME = "AsieLauncher";
	public String URL = "http://127.0.0.1:8080/";
	private String PREFIX = "/.asielauncher/default/";
	public ArrayList<ModFile> baseFiles;
	protected String directory;
	private String OS;
	private JSONObject file, oldFile, configFile;
	private int fullProgress, fullTotal;
	public IProgressUpdater updater;
	public boolean launchedMinecraft = false;
	private MinecraftFrame frame;
	private String loadDir;
	private boolean onlineMode = false;
	private Authentication auth;
	
	public int getFileRevision(JSONObject source) {
		Long revNew = (Long)(source.get("client_revision"));
		if(revNew == null) return 1;
		return revNew.intValue();
	}
	
	public boolean sameClientRevision() {
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
		JSONObject options = (JSONObject)file.get("options");
		for(Object o: options.values()) {
			JSONObject option = (JSONObject)o;
			optionMap.put((String)option.get("id"), option);
		}
		return optionMap;
	}
	public void configureConfig() {
		configFile = readJSONFile(getClass().getResource("/resources/config.json").getPath());
		PREFIX = "/.asielauncher/"+(String)configFile.get("directoryName")+"/";
		URL = (String)configFile.get("serverUrl");
		WINDOW_NAME = (String)configFile.get("windowName");
	}
	public AsieLauncher() {
		configureConfig();
		directory = System.getProperty("user.home") + PREFIX;
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
		return !launchedMinecraft || (frame != null && frame.isAppletActive());
	}
	
	// StackOverflow
	private static void pipeOutput(Process process) {
	    pipe(process.getErrorStream(), System.err);
	    pipe(process.getInputStream(), System.out);
	}

	private static void pipe(final InputStream src, final PrintStream dest) {
	    new Thread(new Runnable() {
	        public void run() {
	            try {
	                byte[] buffer = new byte[1024];
	                for (int n = 0; n != -1; n = src.read(buffer)) {
	                    dest.write(buffer, 0, n);
	                }
	            } catch (IOException e) { // just exit
	            }
	        }
	    }).start();
	}
	
	public ArrayList<String> getMCArguments(String path, String classpath, String username, String sessionID, String jvmArgs) {
		ArrayList<String> args = new ArrayList<String>();
		args.add(path);
		args.addAll(Arrays.asList(jvmArgs.split(" ")));
		args.add("-cp"); args.add(classpath);
		args.add("org.smbarbour.mcu.MinecraftFrame");
		args.add(username); args.add(sessionID); args.add(WINDOW_NAME);
		args.add(new File(directory).getAbsolutePath());
		args.add(new File(directory, "bin").getAbsolutePath());
		args.add("854"); args.add("480");
		args.add("null"); args.add("false");
		System.out.println("Launching with arguments: " + args.toString());
		return args;
	}
	
	public void launch(String _username, String password, String jvmArgs) {
		String username = _username;
		String sessionID = "null";
		if(onlineMode) {
			auth = new AuthenticationMojangLegacy();
			if(!auth.authenticate(username, password)) {
				if(updater != null) updater.setStatus("Login error: " + auth.getErrorMessage());
				return;
			} else {
				username = auth.getUsername();
				sessionID = auth.getSessionID();
			}
		}
		if(sessionID.length() == 0) sessionID = "null";
		String separator = System.getProperty("file.separator");
	    String classpath = System.getProperty("java.class.path");
	    System.out.println(loadDir);
	    System.out.println(classpath);
	    if(classpath.indexOf(separator) == -1 || (loadDir.indexOf("/") == 0 && classpath.indexOf("/") != 0)) {
	    	classpath = (new File(loadDir, classpath)).getAbsolutePath();
	    }
	    System.out.println(classpath);
	    String path = System.getProperty("java.home")
	            + separator + "bin" + separator + Utils.getJavaBinaryName();
		if(updater != null) updater.update(100,100);
		this.setStatus(Strings.LAUNCHING);
	    if((new File(path)).exists()) {
	    	System.out.println("Launching via process spawner");
	    	ProcessBuilder processBuilder = new ProcessBuilder(getMCArguments(path,classpath,username,sessionID,jvmArgs));
	    	try {
	    		processBuilder.directory(new File(directory));
	    		Process process = processBuilder.start();
		    	pipeOutput(process);
	    		try { Thread.sleep(500); } catch(Exception e){}
	    		launchedMinecraft = true;
	    	}
	    	catch(Exception e) { }
	    }
	    if(!launchedMinecraft) {
	    	System.out.println("Launching via internal Java process");
	    	System.setProperty("user.dir", (new File(directory).getAbsolutePath()));
	    	frame = new MinecraftFrame(WINDOW_NAME, new ImageIcon(this.getClass().getResource("/resources/icon.png")));
			frame.launch(new File(directory),
					new File(directory, "bin"),
					username, sessionID,
					"", new Dimension(854, 480), false);
			try { Thread.sleep(500); } catch(Exception e){}
			launchedMinecraft = true;
	    }
	}
}
