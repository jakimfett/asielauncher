package pl.asiekierka.AsieLauncher.download;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class FileParserJSON {
	private String baseURL, baseDirectory;
	
	public FileParserJSON(String baseURL, String baseDirectory) {
		this.baseURL = baseURL;
		this.baseDirectory = baseDirectory;
	}
	
	public ArrayList<FileDownloader> parse(JSONArray jsonList, String mode, String prefix) {
		ArrayList<FileDownloader> list = new ArrayList<FileDownloader>();
		for(Object o: jsonList) {
			if(o instanceof JSONObject) {
				JSONObject mod = (JSONObject) o;
				String filename = (String)mod.get("filename");
				Long filesize = (Long)mod.get("size");
				String md5 = (String)mod.get("md5");
				boolean overwrite = (Boolean)mod.get("overwrite");
				if(mode.equals("http")) {
					String inputAddress = baseURL + prefix + filename;
					String outputFile = baseDirectory + filename;
					FileDownloader fileDownloader = new FileDownloaderHTTP(inputAddress, outputFile, md5, filesize.intValue(), overwrite);
					list.add(fileDownloader);
				} else if(mode.equals("zip")) {
					String inputAddress = baseURL + "zips/" + prefix + filename;
					String outputFile = baseDirectory + "zips/" + filename;
					String unpackLocation = baseDirectory + (String)mod.get("directory") + "/";
					FileDownloader fileDownloader = new FileDownloaderZip(inputAddress, outputFile, unpackLocation, md5, filesize.intValue(), overwrite);
					list.add(fileDownloader);
				}
			}
		}
		return list;
	}
	
	public ArrayList<FileDownloader> parse(JSONArray jsonList, String mode) {
		return parse(jsonList, mode, "");
	}
	
	public ArrayList<FileDownloader> parse(JSONObject object, String mode, String prefix) {
		if(object == null) return new ArrayList<FileDownloader>();
		if(mode.equals("http") && object.containsKey("files")) {
			JSONArray array = (JSONArray)object.get("files");
			return parse(array, mode, prefix);
		} else if(mode.equals("zip") && object.containsKey("zips")) {
			JSONArray array = (JSONArray)object.get("zips");
			return parse(array, mode, prefix);
		} else return new ArrayList<FileDownloader>();
	}
	
	public ArrayList<FileDownloader> parse(JSONObject object, String mode) {
		return parse(object, mode, "");
	}
	
	public ArrayList<FileDownloader> parse(JSONObject object) {
		ArrayList<FileDownloader> files = new ArrayList<FileDownloader>();
		files.addAll(parse(object, "http"));
		files.addAll(parse(object, "zip"));
		return files;
	}
}
