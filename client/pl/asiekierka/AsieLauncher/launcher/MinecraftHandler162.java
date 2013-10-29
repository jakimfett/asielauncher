package pl.asiekierka.AsieLauncher.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.simple.*;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.JavaLauncher;
import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.download.AssetDownloader;

public class MinecraftHandler162 implements MinecraftHandler {
	private AssetDownloader assetDownloader;
	private IProgressUpdater updater;
	private String assetsDir;
	private JSONObject launchInfo;
	private String gameArguments, gameVersion;
	private ArrayList<String> libraries;
	private String mainClass, nativesDir;
	private boolean hasForge = false;
	
	public MinecraftHandler162() {
		// TODO Auto-generated constructor stub
	}
	
	public String getJarLocation(AsieLauncher l) {
		File dir = new File(l.baseDir + "versions/" + l.mcVersion + "/");
		if(!dir.exists()) dir.mkdirs();
		return Utils.getPath(l.baseDir + "versions/" + l.mcVersion + "/minecraft.jar");
	}
	
	public String getNativesLocation(AsieLauncher l) {
		File dir = new File(l.baseDir + "versions/" + l.mcVersion + "/natives/");
		if(!dir.exists()) dir.mkdirs();
		return dir.getAbsolutePath();
	}
	
	public String findForge(File[] jarPatchFiles) {
		if(jarPatchFiles != null) {
			for(File f: jarPatchFiles) {
				if(f.getName().startsWith("minecraftforge")) {
					// Found it!
					Utils.logger.log(Level.FINE, "Found an instance of Forge: " + f.getAbsolutePath());
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}
	
	public void addTweak(String s, String version) {
		if(version.equals("1.6.2")) addTweak162(s);
		else gameArguments += " --tweakClass "+s;
	}
	
    private void addTweak162(String s) {
        if(gameArguments.indexOf("tweakClass") >= 0 || hasForge) {
                gameArguments += " --cascadedTweaks "+s;
        } else {
                gameArguments += " --tweakClass "+s;
                // Install the tweaker
                Utils.logger.log(Level.WARNING, "TODO: INSTALL TWEAKER WITHOUT FORGE; MIGHT GET A BIT MESSY");
        }
    }
    
    public void scanLiteLoader(String modDir, String libraryDir, String version) {
        try {
                File[] loaderFiles = new File(modDir).listFiles();
                if(loaderFiles == null) return;
                for(File f: loaderFiles) {
                        if(f.getName().endsWith(".litemod")) {
                                Utils.logger.log(Level.INFO, "LiteLoader mod '" + f.getName() + "' found, downloading LiteLoader...");
                                addTweak("com.mumfrey.liteloader.launch.LiteLoaderTweaker", version);
                                downloadLibrary("http://dl.liteloader.com/versions/", "com.mumfrey",
                                                "liteloader", version, libraryDir, false);
                                libraries.add(libraryDir+"liteloader-"+version+".jar");
                                return;
                        }
                }
        } catch(Exception e) { e.printStackTrace(); }
}
	
	public boolean downloadLibraries(String patchDir, String libraryDir) {
		ArrayList<String> addedLibraries = new ArrayList<String>();
		// Go through library files
		File jarPatchesDirectory = new File(patchDir, "lib");
		File[] jarPatchFiles = jarPatchesDirectory.listFiles();
		if(jarPatchFiles != null) {
			for(File f: jarPatchFiles) {
				libraries.add(f.getAbsolutePath());
			}
		}
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
			String addon = "";
			if(updater != null) {
				updater.update(i, imax);
			}
			if(addedLibraries.contains(uid)) continue; // We already have this lib
			if(jsonLibrary.containsKey("natives")) {
				// This is a /native/ file, so change filename
				JSONObject nativeList = (JSONObject)jsonLibrary.get("natives");
				addon = (String)nativeList.get(Utils.getSystemName());
				filename = data[1] + "-" + data[2] + "-" + addon + ".jar";
			}
			String filePath = libraryDir + filename; // Don't move that above natives code.
			String urlPrefix = "http://s3.amazonaws.com/Minecraft.Download/libraries/";
			if(jsonLibrary.containsKey("url"))
				urlPrefix = (String)jsonLibrary.get("url");
			if(!(new File(filePath).exists())) {
				boolean found = false;
				if(new File(jarPatchesDirectory, filename).exists()) { // We have one in custom
					found = true;
					hasForge = true;
					filePath = new File(jarPatchesDirectory, filename).getAbsolutePath();
					Utils.logger.log(Level.FINER, "Replacing library " + filename + " with local copy");
				}
				if(filename.startsWith("minecraftforge")) {
					String forgeLocation = findForge(jarPatchFiles);
					if(forgeLocation != null) {
						filePath = forgeLocation;
						found = true;
					}
				}
				if(!found) downloadLibrary(urlPrefix, data[0], data[1], data[2], addon, libraryDir, false);
			}
			if(jsonLibrary.containsKey("natives")) {
				// This is a /native/ file, so extract it
				try {
					Utils.extract(filePath, nativesDir, true);
				} catch(Exception e) { e.printStackTrace(); return false; }
			}
			if(!filename.startsWith("minecraftforge")) libraries.add(filePath);
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
	
	public boolean downloadLibrary(String urlPrefix, String className, String name, String version, String libraryDir, boolean force) {
		return this.downloadLibrary(urlPrefix, className, name, version, "", libraryDir, force);
	}
	
	public boolean downloadLibrary(String urlPrefix, String className, String name, String version, String addon, String libraryDir, boolean force) {
		String filename = name + "-" + version + ".jar";
		if(addon.length() > 0)
			filename = name + "-" + version + "-" + addon + ".jar";
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
	
	public boolean loadJSON(String patchDir, String gameDir, String jsonDir, String version) {
		if(launchInfo != null) return true; // Already been here
		if(updater != null) {
			updater.update(1, 2);
			updater.setStatus(Strings.JSON_CHECKING);
		}
		// Try to find one in per-server libraries
		boolean found = false;
		File customFolder = new File(patchDir, "lib");
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
	
	public boolean checkMinecraft(AsieLauncher l) {
		assetsDir = l.baseDir+"assets/";
		gameVersion = l.mcVersion;
		nativesDir = getNativesLocation(l);
		File dir = new File(l.baseDir + "libraries/custom/");
		if(!dir.exists()) dir.mkdirs();
		if(!loadJSON(l.directory, l.baseDir, l.directory+"launchinfo.json", l.mcVersion)) return false;
		if(!downloadMinecraft(getJarLocation(l), l.mcVersion)) return false;
		return true;
	}
	
	@Override
	public boolean download(AsieLauncher l) {
		assetDownloader = new AssetDownloader(updater);
		assetsDir = l.baseDir+"assets/";
		if(!assetDownloader.download(assetsDir)) return false;
		if(!checkMinecraft(l)) return false;
		return true;
	}
	
	private String generateClasspath(AsieLauncher l) {
		String classpathSeparator = ":";
		if(Utils.getSystemName().equals("windows")) classpathSeparator = ";";
		StringBuilder sb = new StringBuilder();
		sb.append(getJarLocation(l)); // Minecraft.jar
		if(libraries != null) {
			for(String s : libraries) { // Libraries
				sb.append(classpathSeparator);
				sb.append(Utils.getPath(s));
			}
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
	
	public ArrayList<String> getMCArguments(AsieLauncher l, String path, String username, String sessionID, String jvmArgs) {
		ArrayList<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(jvmArgs.split(" ")));
		args.add("-cp"); args.add(generateClasspath(l));
		args.add("-Djava.library.path=" + new File(nativesDir).getAbsolutePath());
        args.add("-Dfml.ignorePatchDiscrepancies=true");
        args.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
		args.add(mainClass);
		// Parse gameArguments
		gameArguments = gameArguments.replaceAll("\\$\\{auth_player_name\\}", username)
				                     .replaceAll("\\$\\{auth_session\\}", sessionID)
				                     .replaceAll("\\$\\{version_name\\}", gameVersion);
		// Fix cascadedTweaks because Forge bugs
		if(l.mcVersion.equalsIgnoreCase("1.6.2")) {
			if(gameArguments.indexOf("--cascadedTweaks") >= 0 // If we have more tweaks
					&& gameArguments.indexOf("--tweakClass cpw.mods.fml.common.launcher.FMLTweaker") >= 0) {
				gameArguments = gameArguments.replaceAll("--tweakClass cpw.mods.fml.common.launcher.FMLTweaker", "")
											 .replaceFirst("--cascadedTweaks", "--tweakClass")
											 + " --cascadedTweaks cpw.mods.fml.common.launcher.FMLTweaker";
			}
		} else {
			gameArguments = gameArguments.replaceAll("--cascadedTweaks", "--tweakClass");
		}
		// Workaround for broken Windows path handling
		String[] gameArgArray = gameArguments.split(" ");
		for(int i = 0; i < gameArgArray.length; i++) {
			if(gameArgArray[i].equals("${game_directory}"))
				gameArgArray[i] = Utils.getPath(new File(path).getAbsolutePath());
			else if(gameArgArray[i].equals("${game_assets}"))
				gameArgArray[i] = Utils.getPath(assetsDir);
		}
		args.addAll(Arrays.asList(gameArgArray));
		Utils.logger.log(Level.INFO, "Launching with arguments: " + args.toString());
		return args;
	}
	
	@Override
	public boolean launch(String path, String username, String sessionID, String jvmArgs, AsieLauncher l) {
		if(!checkMinecraft(l)) return false;
		if(sessionID == null || sessionID.length() == 0) sessionID = "null";
		// Launch Minecraft.
		setStatus(l, Strings.LAUNCHING);
		return JavaLauncher.launch(path, getMCArguments(l, path, username, sessionID, jvmArgs));
	}

	public IProgressUpdater getUpdater() {
		return updater;
	}

	public void setUpdater(IProgressUpdater updater) {
		this.updater = updater;
	}

}
