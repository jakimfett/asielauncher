package pl.asiekierka.AsieLauncher.download;

import java.io.*;

import pl.asiekierka.AsieLauncher.common.IProgressUpdater;
import pl.asiekierka.AsieLauncher.common.Utils;
import pl.asiekierka.AsieLauncher.launcher.Strings;

public class FileDownloaderZip extends FileDownloaderHTTP
{
    public String unpackLocation;

    public FileDownloaderZip(String in, String out, String unpack, String md5, int filesize, boolean overwrite)
    {
        super(in, out, md5, filesize, overwrite);
        this.unpackLocation = unpack;
    }

    @Override
    public boolean install(IProgressUpdater updater)
    {
        boolean downloaded = super.install(updater, true);
        if (!downloaded)
        {
            return false;
        }
        File dirFile = new File(unpackLocation);
        dirFile.mkdir();
        updater.setStatus(Strings.UNPACKING + " " + this.getFilename() + "...");
        try
        {
            System.out.println("Location: " + this.getLocation());
            System.out.println("Destination: " + unpackLocation);
            System.out.println("Overwrite? " + this.isOverwrite());
            Utils.extract(this.getLocation(), unpackLocation, this.isOverwrite());
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean remove()
    {
        return true; // DUMMY! We can't really figure out what we removed, besides, users might have touched it.
    }
}
