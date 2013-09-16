package pl.asiekierka.AsieLauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Repacker {
	private String jarFilename;
	private HashMap<String, String> fileMap;
	public Repacker(String _jarFilename) {
		jarFilename=_jarFilename;
	}
	
	private void mergeJarFiles(String patchName, ZipInputStream in, ZipOutputStream out) throws ZipException, IOException {
		ZipEntry entry = in.getNextEntry();
		while(entry != null) {
			String name = entry.getName();
			if(!name.startsWith("META-INF")) {
				if(fileMap.get(name).equals(patchName)) {
					ZipEntry destEntry = new ZipEntry (entry.getName());
					destEntry.setTime(entry.getTime());
					destEntry.setExtra(entry.getExtra());
					destEntry.setComment(entry.getComment());
					out.putNextEntry(destEntry);
					Utils.copyStream(in, out);
					out.closeEntry();
				}
			}
			entry = in.getNextEntry();
		}
	}
	
	public void merge(String jarFile, String[] patches, String outFile) throws ZipException, IOException {
		fileMap = new HashMap<String, String>();
		// Generate filemap.
		for(String s: Utils.getZipList(jarFile)) {
			fileMap.put(s, jarFile);
		}
		for(String p: patches) {
			for(String s: Utils.getZipList(p)) {
				if(fileMap.containsKey(s)) {
					fileMap.remove(s);
				}
				fileMap.put(s, p);
			}
		}
		// Then finally do the merge.
		File fJarFile = new File(jarFile);
		File fOutFile = new File(outFile);
		ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(fOutFile));
		ZipInputStream jarInStream = new ZipInputStream(new FileInputStream(fJarFile));
		mergeJarFiles(jarFile, jarInStream, outStream);
		Utils.close(jarInStream);
		// Apply file patches
		for(String patch: patches) {
			File patchFile = new File(patch);
			ZipInputStream patchIn = new ZipInputStream(new FileInputStream(patchFile));
			mergeJarFiles(patch, patchIn, outStream);
			Utils.close(patchIn);
		}
		Utils.close(outStream);
	}
	
	public boolean repackJar(String mcFile, String[] patches) {
		try {
			File fMCFile = new File(jarFilename);
			if(fMCFile.exists()) fMCFile.delete();
			merge(mcFile, patches, jarFilename);
		} catch(Exception e) { e.printStackTrace(); return false; }
		return true;
	}
}
