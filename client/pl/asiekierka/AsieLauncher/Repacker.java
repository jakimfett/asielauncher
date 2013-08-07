package pl.asiekierka.AsieLauncher;

import java.io.File;

import net.lingala.zip4j.core.*;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class Repacker {
	private String directory, tempDirectory, jarFilename;
	
	public Repacker(String _directory, String _jarFilename) {
		directory=_directory;
		jarFilename=_jarFilename;
		tempDirectory = new File(directory, "temp").getAbsolutePath();
		File dirFile = new File(directory);
		if(!dirFile.exists()) dirFile.mkdirs();
		else Utils.deleteFolderContents(dirFile);
		File tempFile = new File(tempDirectory);
		tempFile.mkdirs();
	}
	
	public boolean unpackZips(String[] filenames) {
		for(String s: filenames) {
			try {
				System.out.println("[Repack] "+s+" => "+tempDirectory);
				Utils.extract(s, tempDirectory, true);
			} catch(Exception e) { e.printStackTrace(); return false; }
		}
		return true;
	}
	
	public boolean repackJar() {
		File metaInf = new File(tempDirectory, "META-INF");
		if(metaInf.exists()) {
			Utils.deleteFolderContents(metaInf);
			metaInf.delete();
		}
		if((new File(jarFilename)).exists()) {
			(new File(jarFilename)).delete();
		}
		try {
			ZipFile zipFile = new ZipFile(jarFilename);
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FAST);
			parameters.setIncludeRootFolder(false);
			zipFile.createZipFileFromFolder(new File(tempDirectory), parameters, false, (2^31));
		} catch(Exception e) { e.printStackTrace(); return false; }
		return true;
	}
}
