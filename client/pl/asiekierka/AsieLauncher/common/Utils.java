package pl.asiekierka.AsieLauncher.common;

import java.awt.Toolkit;
import java.io.*;
import java.math.*;
import java.net.URL;
import java.security.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Utils {
	private static final int BUFFER = 65536;
	public static Logger logger;
	
	static {
		logger = Logger.getLogger(Utils.class.getName());
		logger.addHandler(new ConsoleHandler());
	}
	
	public static String getPath(String path) {
		if(path == null) return null;
		return path.replaceAll("\\\\|/", "\\"+System.getProperty("file.separator"));
	}
	
	public static String getSystemName() {
		String name = System.getProperty("os.name");
		//if(name.indexOf("Solaris") >= 0) return "solaris"; -- Deprecated in 1.6.2+
		if(name.indexOf("Linux") >= 0) return "linux";
		if(name.indexOf("Mac") >= 0) return "osx";
		if(name.indexOf("Windows") >= 0) return "windows";
		return "commodore64";
		// Default choice.
		// Most likely as the C64 was the best selling home computer in history.
		// ToDon't: Check the SID version for the zealots.
	}
	
	public static int versionToInt(String ver) {
		String[] stringParts = ver.split("\\.");
		int version = 0;
		for(int i = 0; i < 4; i++) {
			version *= 128;
			if(i >= stringParts.length) continue;
			version += new Integer(stringParts[i]);
		}
		return version;
	}
	
	public static boolean saveStringToFile(String filename, String data) {
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
	
	public static String loadStringFromFile(String filename) {
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
	
	// StackOverflow
	public static void pipeOutput(Process process) {
	    pipe(process.getErrorStream(), System.err);
	    pipe(process.getInputStream(), System.out);
	}

	public static void pipe(final InputStream src, final PrintStream dest) {
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
	
	public static String getJavaBinaryName() {
		if(getSystemName().equals("windows")) return "java.exe";
		else if(getSystemName().equals("commodore64")) return "JAVA.PRG";
		else return "java";
	}
	
	public static double getScaleFactor() {
		Object appleScaleFactor = Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor");
		if(appleScaleFactor instanceof String) {
			return Double.parseDouble((String)appleScaleFactor);
		} else if(appleScaleFactor instanceof Float) {
			return (double)((Float)appleScaleFactor);
		}
		return 1.0;
	}
	public static String fixURLString(String old) {
		return old.replaceAll(" ", "%20");
	}
	
	public static String[] getZipList(String zipFile) throws ZipException, IOException {
	    File file = new File(zipFile);
	    ZipFile zip = new ZipFile(file);
	    Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
	    ArrayList<String> files = new ArrayList<String>();
	    
	    // Process each entry
	    while (zipFileEntries.hasMoreElements())
	    {
	        // grab a zip file entry
	        ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
	        String currentEntry = entry.getName();
	        files.add(currentEntry);
	    }
	    return files.toArray(new String[files.size()]);
	}
	
	public static String md5(File file)
			throws NoSuchAlgorithmException,
			FileNotFoundException, IOException
	{
		MessageDigest md = MessageDigest.getInstance("MD5");
		 
		InputStream is= new FileInputStream(file);
		 
		byte[] buffer=new byte[8192];
		int read=0;
		 
		while( (read = is.read(buffer)) > 0)
			md.update(buffer, 0, read);

		byte[] md5 = md.digest();
		BigInteger bi=new BigInteger(1, md5);
		 
		is.close();
		String hex = bi.toString(16);
		while(hex.length() < 32) { hex = "0" + hex; } // Padding
		return hex;
	}
	
	public static JSONObject readJSONFile(String filename) {
		try {
			JSONParser tmp = new JSONParser();
			Object o = tmp.parse(new InputStreamReader(new FileInputStream(filename)));
			if(!(o instanceof JSONObject)) return null;
			else return (JSONObject)o;
		} catch(Exception e) { e.printStackTrace(); return null; }
	}
	
	public static JSONObject readJSONUrlFile(URL url) {
		try {
			JSONParser tmp = new JSONParser();
			Object o = tmp.parse(new InputStreamReader(url.openStream()));
			if(!(o instanceof JSONObject)) return null;
			else return (JSONObject)o;
		} catch(Exception e) { e.printStackTrace(); return null; }
	}
	
	public static JSONObject readJSONUrlFile(String url) {
		try {
			return readJSONUrlFile(new URL(url));
		} catch(Exception e) { e.printStackTrace(); return null; }
	}
	
	public static boolean download(URL url, String file) {
		return download(url,file,0,0,null);
	}
	public static boolean download(URL url, String file, int filesize, IProgressUpdater updater) {
		return download(url,file, 0, filesize, updater);
	}
	public static boolean download(URL url, String file, int prefix, int totalFilesize, IProgressUpdater updater) {
		File fFile = new File(new File(file).getParentFile().getPath());
		if(!fFile.exists()) fFile.mkdirs();
 		boolean downloaded = true;
    	BufferedInputStream in = null;
    	FileOutputStream out = null;
    	BufferedOutputStream bout = null;
    	try {
    		int count;
    		int totalCount = 0;
    		byte data[] = new byte[BUFFER];
    		in = new BufferedInputStream(url.openStream());
    		out = new FileOutputStream(file);
    		bout = new BufferedOutputStream(out);
    		while ((count = in.read(data, 0, BUFFER)) != -1) {
    			bout.write(data, 0, count);
    			totalCount += count;
    			if(updater != null) updater.update(prefix+totalCount, totalFilesize);
    		}
    	}
    	catch(Exception e) { e.printStackTrace(); Utils.logger.log(Level.SEVERE, "Download error!"); downloaded = false; }
    	finally {
    		try {
    			close(in);
    			close(bout);
    			close(out);
    		} catch(Exception e) { e.printStackTrace(); }
    	}
    	return downloaded;
	}
	// From StackOverflow
	public static void deleteFolderContents(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolderContents(f);
	            }
	            f.delete();
	        }
	    }
	}
	
	public static void close(Object o) {
		try {
			if(o == null) return;
			if(o instanceof InputStream) {
				((InputStream)o).close();
			}
			else if(o instanceof OutputStream) {
				((OutputStream)o).flush();
				((OutputStream)o).close();
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] data = new byte[BUFFER];
		int currentByte;
		while ((currentByte = in.read(data, 0, BUFFER)) != -1) {
			out.write(data, 0, currentByte);
		}
	}
	
	public static void extract(String zipFile, String newPath, boolean overwrite) throws ZipException, IOException {
	    File file = new File(zipFile);

	    ZipFile zip = new ZipFile(file);

	    new File(newPath).mkdir();
	    Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();

	    // Process each entry
	    while (zipFileEntries.hasMoreElements())
	    {
	        // grab a zip file entry
	        ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
	        String currentEntry = entry.getName();
	        File destFile = new File(newPath, currentEntry);
	        //destFile = new File(newPath, destFile.getName());
	        File destinationParent = destFile.getParentFile();

	        // create the parent directory structure if needed
	        destinationParent.mkdirs();

	        if (!entry.isDirectory())
	        {
	        	if(!overwrite && destFile.exists()) continue;
	            BufferedInputStream is = new BufferedInputStream(zip
	            .getInputStream(entry));
	            int currentByte;
	            // establish buffer for writing file
	            byte data[] = new byte[BUFFER];

	            // write the current file to disk
	            FileOutputStream fos = new FileOutputStream(destFile);
	            BufferedOutputStream dest = new BufferedOutputStream(fos,
	            BUFFER);

	            // read and write until last byte is encountered
	            while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
	                dest.write(data, 0, currentByte);
	            }
	            close(dest);
	            close(is);
	        }
	    }
	}
}
