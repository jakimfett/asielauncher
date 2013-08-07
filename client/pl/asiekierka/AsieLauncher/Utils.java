package pl.asiekierka.AsieLauncher;

import java.awt.Toolkit;
import java.io.*;
import java.math.*;
import java.net.URL;
import java.security.*;
import java.util.*;
import java.util.zip.*;

public class Utils {

	public static String getSystemName() {
		String name = System.getProperty("os.name");
		if(name.indexOf("Solaris") >= 0) return "solaris";
		if(name.indexOf("Linux") >= 0) return "linux";
		if(name.indexOf("Mac") >= 0) return "macosx";
		if(name.indexOf("Windows") >= 0) return "windows";
		return "commodore64";
		// Default choice.
		// Most likely as the C64 was the best selling home computer in history.
		// ToDon't: Check the SID version for the zealots.
	}
	
	public static int versionToString(String ver) {
		String[] stringParts = ver.split("\\.");
		int version = 0;
		for(int i = 0; i < 4; i++) {
			version *= 128;
			if(i >= stringParts.length) continue;
			version += new Integer(stringParts[i]);
		}
		return version;
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
	
	public static boolean download(URL url, String file) {
		return download(url,file,0,null);
	}
	public static boolean download(URL url, String file, int filesize, IProgressUpdater updater) {
		boolean downloaded = true;
    	BufferedInputStream in = null;
    	FileOutputStream out = null;
    	try {
    		int count;
    		int totalCount = 0;
    		byte data[] = new byte[1024];
    		in = new BufferedInputStream(url.openStream());
    		out = new FileOutputStream(file);
    		while ((count = in.read(data, 0, 1024)) != -1) {
    			out.write(data, 0, count);
    			totalCount += count;
    			if(updater != null) updater.update(totalCount, filesize);
    		}
    	}
    	catch(Exception e) { e.printStackTrace(); System.out.println("Download error!"); downloaded = false; }
    	finally {
    		try {
    			if (in != null) in.close();
    			if (out != null) out.close();
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
	public static void extract(String zipFile, String newPath, boolean overwrite) throws ZipException, IOException 
	{
	    int BUFFER = 4096;
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
	            dest.flush();
	            dest.close();
	            is.close();
	        }
	    }
	}
}
