package pl.asiekierka.AsieLauncher.download;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class AssetDownloader implements IProgressUpdater {

	private String assetURL;
	private IProgressUpdater updater;
	private int realTotal, currTotal;
	
	public AssetDownloader(String assetURL, IProgressUpdater updater) {
		this.assetURL = assetURL;
		this.updater = updater;
	}
	public AssetDownloader(IProgressUpdater updater) {
		this("http://resources.download.minecraft.net/", updater);
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
		// HACK: Compare by filesize only.
		// I know it's the dumbest way to do it, but it's sufficient for assets and
		// I can't bother reading through the ETag docs or parsing dates. Heh.
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new URL(assetURL).openStream());
			// Welcome to DOM. The land of torture.
			NodeList files = doc.getElementsByTagName("Contents");
			ArrayList<FileDownloader> filelist = new ArrayList<FileDownloader>(files.getLength());
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
				filelist.add(new FileDownloaderHTTP(assetURL + filename, directory + filename, "", filesize, true));
				realTotal += filesize;
			}
			return filelist;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
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
