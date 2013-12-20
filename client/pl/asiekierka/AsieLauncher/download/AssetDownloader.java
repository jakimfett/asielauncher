package pl.asiekierka.AsieLauncher.download;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class AssetDownloader implements IProgressUpdater {

	private String indexName;
	private IProgressUpdater updater;
	private int realTotal, currTotal;
	
	public AssetDownloader(String assetURL, IProgressUpdater updater) {
		this.indexName = assetURL;
		this.updater = updater;
	}
	public AssetDownloader(IProgressUpdater updater) {
		this("legacy", updater);
	}
	public AssetDownloader(String assetURL) {
		this(assetURL, null);
	}
	public AssetDownloader() {
		this((IProgressUpdater)null);
	}

	public ArrayList<FileDownloader> list(String directory) {
		if(updater != null) {
			updater.update(0, 2);
			updater.setStatus(Strings.ASSET_CHECKING);
		}
		JSONObject obj = Utils.readJSONUrlFile("http://s3.amazonaws.com/Minecraft.Download/indexes/"+indexName+".json");
		JSONObject objects = (JSONObject)obj.get("objects");
		ArrayList<FileDownloader> filelist = new ArrayList<FileDownloader>();
		for(Object o: objects.keySet()) {
			String filename = (String)o;
			JSONObject data = (JSONObject)objects.get(filename);
			int filesize = ((Long)data.get("size")).intValue();
			String filehash = (String)data.get("hash");
			if(filename.endsWith("/")) continue; // Is secretly a directory.
			String downloadURL = "http://resources.download.minecraft.net/"+filehash.substring(0, 2)+"/"+filehash;
			filelist.add(new FileDownloaderHTTP(downloadURL, directory + filename, "", filesize, true));
			realTotal += filesize;
		}
		return filelist;
	}
	
	public boolean download(String directory) {
		ArrayList<FileDownloader> filelist = list(directory);
		if(filelist == null) return false;
		for(FileDownloader file: filelist) {
			if(!file.install(this)) return false;
			currTotal += file.getFilesize();
		}
		return true;
	}
	
	@Override
	public void update(int a, int b) {
		if(updater != null) updater.update(currTotal+a, realTotal);
	}
	@Override
	public void setStatus(String status) {
		updater.setStatus(status);
	}
}
