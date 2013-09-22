package pl.asiekierka.AsieLauncher.downloader;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.launcher.IProgressUpdater;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class AssetDownloader {

	private String assetURL;
	private IProgressUpdater updater;
	
	public AssetDownloader(String assetURL, IProgressUpdater updater) {
		this.assetURL = assetURL;
		this.updater = updater;
	}
	public AssetDownloader(IProgressUpdater updater) {
		this("https://s3.amazonaws.com/Minecraft.Resources/", updater);
	}
	public AssetDownloader(String assetURL) {
		this(assetURL, null);
	}
	public AssetDownloader() {
		this((IProgressUpdater)null);
	}

	public boolean download(String directory) {
		if(updater != null) {
			updater.update(0, 2);
			updater.setStatus(Strings.ASSET_CHECKING);
		}
		// HACK: Compare by filesize only.
		// I know it's the dumbest way to do it, but it's sufficient for assets and
		// I can't bother reading through the ETag docs or parsing dates. Heh.
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new URL(assetURL).openStream());
			// Welcome to DOM. The land of torture.
			NodeList files = doc.getElementsByTagName("Contents");
			int totalFilesize = 0;
			int currFilesize = 0;
			HashMap<String, Integer> fileMap = new HashMap<String, Integer>(files.getLength());
			for(int i=0; i<files.getLength(); i++) {
				Node file = files.item(i);
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
				if(!Utils.download(new URL(assetURL + filename), directory + filename, currFilesize, totalFilesize, updater)) {
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
}
