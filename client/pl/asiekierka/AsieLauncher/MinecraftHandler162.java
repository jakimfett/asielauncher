package pl.asiekierka.AsieLauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.*;

import org.json.simple.*;
import org.w3c.dom.*;

public class MinecraftHandler162 implements MinecraftHandler {
	private static final String ASSETS_URL = "https://s3.amazonaws.com/Minecraft.Resources/";
	private IProgressUpdater updater;
	private String assetsDir;
	private boolean hasForge = false;
	
	public MinecraftHandler162() {
		// TODO Auto-generated constructor stub
	}
	
	public String getJarLocation(AsieLauncher l, String version) {
		File dir = new File(l.baseDir + "versions/" + version + "/");
		if(!dir.exists()) dir.mkdirs();
		return l.baseDir + "versions/" + version + "/minecraft.jar";
	}
	
	public String getNativesLocation(AsieLauncher l, String version) {
		File dir = new File(l.baseDir + "versions/" + version + "/natives/");
		if(!dir.exists()) dir.mkdirs();
		return l.baseDir + "versions/" + version + "/natives/";
	}
	
	public boolean downloadAssets(String directory) {
		if(updater != null) {
			updater.update(0, 2);
			updater.setStatus(Strings.ASSET_CHECKING);
		}
		assetsDir = directory; // remember for later on
		// HACK: Compare by filesize only.
		// I know it's the dumbest way to do it, but it's sufficient for assets and
		// I can't bother reading through the ETag docs or parsing dates. Heh.
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new URL(ASSETS_URL).openStream());
			// Welcome to DOM. The land of torture.
			NodeList files = doc.getElementsByTagName("Contents");
			int totalFilesize = 0;
			int currFilesize = 0;
			HashMap<String, Integer> fileMap = new HashMap<String, Integer>(files.getLength());
			for(int i=0; i<files.getLength(); i++) {
				Node file = files.item(i);
				System.out.println("Current element: " + file.getNodeName());
				NodeList fileNodes = file.getChildNodes();
				String filename = null;
				int filesize = -1;
				for(int j=0; j<fileNodes.getLength(); j++) {
					Node fileInformation = fileNodes.item(j);
					if(fileInformation.getTextContent() == null) continue;
					if(fileInformation.getNodeName().equals("Key")) {
						filename = fileInformation.getTextContent();
					} else if(fileInformation.getNodeName().equals("Size")) {
						filesize = new Integer(fileInformation.getTextContent());
					}
				}
				if(filename == null || filesize == -1) continue; // Invalid data.
				if(filename.endsWith("/")) continue; // Is secretly a directory.
				fileMap.put(filename, filesize);
				totalFilesize += filesize;
			}
			// Out of DOM we go, never to remember it again.
			for(String filename: fileMap.keySet()) {
				if(updater != null) updater.setStatus(Strings.DOWNLOADING+" assets/"+filename+" ...");
				File file = new File(directory + filename);
				int filesize = fileMap.get(filename);
				if(file.exists() && file.length() == filesize) {
					currFilesize += filesize;
					if(updater != null) updater.update(currFilesize, totalFilesize);
					continue; // We have this one!
				}
				if(!Utils.download(new URL(ASSETS_URL + filename), directory + filename, currFilesize, totalFilesize, updater)) {
					return false;
				}
				currFilesize += filesize;
			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private JSONObject launchInfo;
	private String gameArguments;
	private String gameVersion;
	private String nativesDir;
	private ArrayList<String> libraries;
	private String mainClass;
	
	public boolean downloadLibraries(String patchDir, String libraryDir) {
		ArrayList<String> addedLibraries = new ArrayList<String>();
		// Go through libraries in JSON file
		JSONArray jsonLibraries = (JSONArray)launchInfo.get("libraries");
		int imax = jsonLibraries.size();
		int i = -1;
		for(Object o: jsonLibraries) {
			i++;
			JSONObject jsonLibrary = (JSONObject)o;
			String[] data = ((String)jsonLibrary.get("name")).split(":");
			String uid = data[0] + ":" + data[1];
			String filename = data[1] + "-" + data[2] + ".jar";
			if(updater != null) {
				updater.update(i, imax);
			}
			if(addedLibraries.contains(uid)) continue; // We already have this lib
			if(jsonLibrary.containsKey("natives")) {
				// This is a /native/ file!
				JSONObject nativeList = (JSONObject)jsonLibrary.get("natives");
				String addon = (String)nativeList.get(Utils.getSystemName());
				filename = data[1] + "-" + data[2] + "-" + addon + ".jar";
			}
			String urlPrefix = "http://s3.amazonaws.com/Minecraft.Download/libraries/";
			if(jsonLibrary.containsKey("url"))
				urlPrefix = (String)jsonLibrary.get("url");
			String filePath = libraryDir + filename;
			if(!(new File(filePath).exists())) {
				boolean found = false;
				if(filename.startsWith("minecraftforge")) {
					// Forge workaround
					File jarPatchesDirectory = new File(patchDir, "jarPatches");
					File[] jarPatchFiles = jarPatchesDirectory.listFiles();
					if(jarPatchFiles != null) {
						for(File f: jarPatchFiles) {
							if(f.getName().startsWith("minecraftforge")) {
								// Found it!
								System.out.println("Found an instance of Forge: " + f.getAbsolutePath());
								filePath = f.getAbsolutePath();
								found = true;
							}
							if(found) break;
						}
					}
				}
				if(!found) downloadLibrary(urlPrefix, data[0], data[1], data[2], libraryDir, false);
			}
			if(jsonLibrary.containsKey("natives")) {
				// This is a /native/ file, so extract it
				try {
					Utils.extract(filePath, nativesDir, true);
				} catch(Exception e) { e.printStackTrace(); return false; }
			}
			libraries.add(filePath);
			addedLibraries.add(uid);
		}
		// Go through "custom" libraries
		File customFolder = new File(libraryDir, "custom");
		File[] customLibraries = customFolder.listFiles();
		for(File library : customLibraries) {
			libraries.add(library.getAbsolutePath());
		}
		// All done.
		return true;
	}
	
	public void addTweak(String s) {
		if(gameArguments.indexOf("tweakClass") >= 0 || hasForge) {
			gameArguments += " --cascadedTweaks "+s;
		} else {
			gameArguments += " --tweakClass "+s;
			// Install the tweaker
			System.out.println("TODO: INSTALL TWEAKER WITHOUT FORGE; MIGHT GET A BIT MESSY");
		}
	}
	
	public boolean downloadLibrary(String urlPrefix, String className, String name, String version, String libraryDir, boolean force) {
		String filename = name + "-" + version + ".jar";
		String filePath = libraryDir + filename;
		if(libraries.contains(filePath)) return true; // Already downloaded
 		String urlPath = urlPrefix + (urlPrefix.endsWith("/")?"":"/") + className.replace('.', '/') + "/"
							+ name + "/" + version + "/" + filename;
		if(force || !(new File(filename)).exists()) {
			if(updater != null) {
				updater.setStatus(Strings.DOWNLOADING + " " + name + " " + version + "...");
			}
			try {
				boolean downloaded = Utils.download(new URL(urlPath), filePath);
				return downloaded;
			} catch(Exception e) { e.printStackTrace(); return false; }
		} else return true;
	}
	
	public void scanLiteLoader(String modDir, String libraryDir, String version) {
		try {
			File[] loaderFiles = new File(modDir).listFiles();
			for(File f: loaderFiles) {
				if(f.getName().endsWith(".litemod")) {
					System.out.println("LiteLoader mod found; downloading LiteLoader...");
					addTweak("com.mumfrey.liteloader.launch.LiteLoaderTweaker");
					downloadLibrary("http://dl.liteloader.com/versions/", "com.mumfrey",
							"liteloader", version, libraryDir, false);
					libraries.add(libraryDir+"liteloader-"+version+".jar");
					return;
				}
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public boolean loadJSON(String patchDir, String gameDir, String jsonDir, String version) {
		if(updater != null) {
			updater.update(1, 2);
			updater.setStatus(Strings.JSON_CHECKING);
		}
		// Try to find one in jarPatches
		boolean found = false;
		File customFolder = new File(patchDir, "jarPatches");
		File[] customPatches = customFolder.listFiles();
		if(new File(patchDir, "version.json").exists()) {
			found = true;
			try {
				Utils.copyStream(new FileInputStream(new File(patchDir, "version.json")),
								new FileOutputStream(new File(jsonDir)));
			} catch(Exception e) { e.printStackTrace(); found = false; }
		}
		if(!found && customPatches != null) {
			for(File patchFile: customPatches) {
				if(found) break;
				try {
					for(String s: Utils.getZipList(patchFile.getAbsolutePath())) {
						if(s.equals("version.json")) {
							found = true; // Got one!
							hasForge = true;
							// Now to unpack it
						    ZipFile zip = new ZipFile(patchFile);
						    Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
						    while (zipFileEntries.hasMoreElements())
						    {
						        // grab a zip file entry
						        ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
						        String currentEntry = entry.getName();
						        if(!currentEntry.equals("version.json")) continue;
						        File destFile = new File(jsonDir);
						        Utils.copyStream(zip.getInputStream(entry), new FileOutputStream(destFile));
						        break;
						    }
						    zip.close();
						    break;
						} else continue;
					}
				} catch(Exception e) { e.printStackTrace(); }
			}
		}
		if(!found) {
			// Download
			try {
				boolean downloaded = Utils.download(new URL("http://s3.amazonaws.com/Minecraft.Download/versions/"+version+"/"+version+".json"), jsonDir);
				if(!downloaded) return false;
			} catch(Exception e) { e.printStackTrace(); return false; }
		}
		// Load
		launchInfo = Utils.readJSONFile(jsonDir);
		if(launchInfo == null) return false;
		// Parse
		gameArguments = (String)launchInfo.get("minecraftArguments"); // Arguments are parsed in getMCArguments();
		mainClass = (String)launchInfo.get("mainClass");
		if(gameArguments.indexOf("cpw.mods.fml") >= 0) {
			hasForge = true; // Found Forge in arguments
		}
		// Libraries
		libraries = new ArrayList<String>();
		scanLiteLoader(patchDir + "mods/", gameDir + "libraries/", version);
		if(!downloadLibraries(patchDir, gameDir + "libraries/")) return false; // Get necessary libraries
		return true;
	}
	
	public boolean downloadMinecraft(String location, String version) {
		if(!(new File(location).exists())) {
			try {
				if(updater != null) updater.setStatus(Strings.DOWNLOADING+" Minecraft...");
				boolean downloaded = Utils.download(new URL("http://s3.amazonaws.com/Minecraft.Download/versions/"+version+"/"+version+".jar"), location);
				return downloaded;
			} catch(Exception e) { e.printStackTrace(); return false; }
		}
		return true;
	}
	
	@Override
	public boolean download(AsieLauncher l, String version) {
		gameVersion = version;
		nativesDir = getNativesLocation(l, version);
		File dir = new File(l.baseDir + "libraries/custom/");
		if(!dir.exists()) dir.mkdirs();
		if(!downloadAssets(l.baseDir+"assets/")) return false;
		if(!loadJSON(l.directory, l.baseDir, l.baseDir+"launchinfo.json", version)) return false;
		if(!downloadMinecraft(getJarLocation(l, version), version)) return false;
		return true;
	}
	
	private String generateClasspath(AsieLauncher l) {
		String classpathSeparator = ":";
		if(Utils.getSystemName().equals("windows")) classpathSeparator = ";";
		StringBuilder sb = new StringBuilder();
		sb.append(getJarLocation(l, l.mcVersion)); // Minecraft.jar
		for(String s : libraries) { // Libraries
			sb.append(classpathSeparator);
			sb.append(s);
		}
		return sb.toString();
	}
	
	@Override
	public boolean isActive() {
		return false; // We only launch via Java thread for now.
	}
	
	private void setStatus(AsieLauncher l, String status) {
		if(l.updater != null) l.updater.setStatus(status);
	}
	
	public ArrayList<String> getMCArguments(AsieLauncher l, String path, String jarPath, String classpath, String username, String sessionID, String jvmArgs) {
		ArrayList<String> args = new ArrayList<String>();
		args.add(jarPath);
		args.addAll(Arrays.asList(jvmArgs.split(" ")));
		args.add("-cp"); args.add(generateClasspath(l));
		args.add("-Djava.library.path=" + new File(nativesDir).getAbsolutePath());
        args.add("-Dfml.ignorePatchDiscrepancies=true");
        args.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
		args.add(mainClass);
		// Parse gameArguments
		gameArguments = gameArguments.replaceAll("\\$\\{auth_player_name\\}", username)
				                     .replaceAll("\\$\\{auth_session\\}", sessionID)
				                     .replaceAll("\\$\\{version_name\\}", gameVersion)
				                     .replaceAll("\\$\\{game_directory\\}", new File(path).getAbsolutePath())
				                     .replaceAll("\\$\\{game_assets\\}", assetsDir);
		args.addAll(Arrays.asList(gameArguments.split(" ")));
		System.out.println("Launching with arguments: " + args.toString());
		return args;
	}
	
	@Override
	public boolean launch(String path, String username, String sessionID, String jvmArgs, AsieLauncher l) {
		if(sessionID.length() == 0) sessionID = "null";
		// Launch Minecraft.
		String separator = System.getProperty("file.separator");
	    String classpath = System.getProperty("java.class.path");
	    System.out.println(l.getLoadDir());
	    System.out.println(classpath);
	    if(classpath.indexOf(separator) == -1 || (l.getLoadDir().indexOf("/") == 0 && classpath.indexOf("/") != 0)) {
	    	classpath = (new File(l.getLoadDir(), classpath)).getAbsolutePath();
	    }
	    System.out.println(classpath);
	    String jarPath = System.getProperty("java.home")
	            + separator + "bin" + separator + Utils.getJavaBinaryName();
		setStatus(l, Strings.LAUNCHING);
	    if((new File(jarPath)).exists()) {
	    	System.out.println("Launching via process spawner");
	    	ProcessBuilder processBuilder = new ProcessBuilder(getMCArguments(l,path,jarPath,classpath,username,sessionID,jvmArgs));
	    	try {
	    		processBuilder.directory(new File(path));
	    		Process process = processBuilder.start();
		    	Utils.pipeOutput(process);
	    		try { Thread.sleep(500); } catch(Exception e){}
	    		return true;
	    	}
	    	catch(Exception e) { }
	    }
	    return true;
	}

	public IProgressUpdater getUpdater() {
		return updater;
	}

	public void setUpdater(IProgressUpdater updater) {
		this.updater = updater;
	}

}
