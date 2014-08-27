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

public class MinecraftHandler1710 extends MinecraftHandler172 {
	public MinecraftHandler1710() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getIndexName() { return "1.7.10"; }
	
	protected ArrayList<String> getMCArguments(AsieLauncher l, String path, String username, String sessionID, String UUID, String jvmArgs) {
		ArrayList<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(jvmArgs.split(" ")));
		args.add("-cp"); args.add(generateClasspath(l));
		args.add("-Djava.library.path=" + new File(nativesDir).getAbsolutePath());
        args.add("-Dfml.ignorePatchDiscrepancies=true");
        args.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
		args.add(mainClass);
		// Parse gameArguments
		gameArguments = gameArguments.replaceAll("\\$\\{auth_player_name\\}", username)
				                     .replaceAll("\\$\\{auth_access_token\\}", sessionID)
				                     .replaceAll("\\$\\{version_name\\}", gameVersion)
				                     .replaceAll("\\$\\{auth_uuid\\}", UUID)
				                     .replaceAll("\\$\\{user_properties\\}", "{}")
				                     .replaceAll("\\$\\{user_type\\}", "mojang")
									 .replaceAll("--cascadedTweaks", "--tweakClass");
		// Workaround for broken Windows path handling
		String[] gameArgArray = gameArguments.split(" ");
		for(int i = 0; i < gameArgArray.length; i++) {
			if(gameArgArray[i].equals("${game_directory}"))
				gameArgArray[i] = Utils.getPath(new File(path).getAbsolutePath());
			else if(gameArgArray[i].equals("${assets_root}"))
				gameArgArray[i] = Utils.getPath(l.baseDir + "assets");
			else if(gameArgArray[i].equals("${assets_index_name}"))
				gameArgArray[i] = getIndexName();
		}
		args.addAll(Arrays.asList(gameArgArray));
		Utils.logger.log(Level.INFO, "Launching with arguments: " + args.toString());
		return args;
	}
}
