// License: https://bitbucket.org/prupe/mcpatcher/src/f179eaa9ed844bf3e4b2600e0057af06540abbd9/LICENSE?at=master

package com.prupe.mcpatcher;

// Class for filler stuff
import pl.asiekierka.AsieLauncher.Utils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import LZMA.*;

public class ForgeAdapter {
    private static final String GDIFF_CLASS = "cpw.mods.fml.repackage.com.nothome.delta.GDiffPatcher";
    private static final String GDIFF_PATCH_METHOD = "patch";

    private static final String BINPATCHES_PACK = "binpatches.pack.lzma";
    private static final String BINPATCH_PREFIX = "binpatch/client/";
    private static final String BINPATCH_SUFFIX = ".binpatch";

    private final File universalJar;
    private final JarFile minecraftJar;
    private final Map<String, Binpatch> patches = new HashMap<String, Binpatch>();
    private ZipFile universalZip;

    private ClassLoader classLoader;

    private Constructor<?> gdiffConstructor1;
    private Constructor<?> gdiffConstructor2;
    private Method gdiffPatchMethod;

    public ForgeAdapter(File universalJar, JarFile minecraftJar) throws Exception {
        this.universalJar = universalJar;
        this.minecraftJar = minecraftJar;
        
        setupClassLoader();
        
        try {
            universalZip = new ZipFile(universalJar);
            loadBinPatches(universalZip, BINPATCHES_PACK);
        } catch(Exception e) { e.printStackTrace(); }
        System.out.println(String.format("[ForgeAdapter] %d classes modified", patches.size()));
    }
    
    public Map<String, Binpatch> getPatches() {
    	return patches;
    }
    
    public InputStream openFile(String name) throws IOException {
        name = name.replaceFirst("^/", "");
        Binpatch binpatch = patches.get(name);
        if (binpatch == null) {
            throw new IOException("No patch for " + name);
        }
        byte[] input = binpatch.getInput(minecraftJar);
        byte[] output = binpatch.apply(input);
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        tmp.write(output);
        return new ByteArrayInputStream(tmp.toByteArray());
    }

    private void setupClassLoader() throws Exception {
        classLoader = URLClassLoader.newInstance(new URL[]{
            universalJar.toURI().toURL()
        }, getClass().getClassLoader());

        Class<?> gdiffClass = classLoader.loadClass(GDIFF_CLASS);
        try {
            gdiffConstructor1 = gdiffClass.getDeclaredConstructor();
            gdiffPatchMethod = gdiffClass.getDeclaredMethod(GDIFF_PATCH_METHOD, byte[].class, InputStream.class, OutputStream.class);
        } catch (NoSuchMethodException e) {
            gdiffConstructor2 = gdiffClass.getDeclaredConstructor(byte[].class, InputStream.class, OutputStream.class);
        }
    }

    private void loadBinPatches(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException("Could not load " + name + " from " + universalJar.getName());
        }
        InputStream raw = null;
        InputStream lzma = null;
        ZipInputStream zipStream = null;
        JarOutputStream jar = null;
        try {
            raw = zip.getInputStream(entry);
            lzma = new LzmaInputStream(raw);
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            jar = new JarOutputStream(byteOutput);
            Pack200.Unpacker pack = Pack200.newUnpacker();
            pack.unpack(lzma, jar);
            jar.close();
            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
            zipStream = new ZipInputStream(byteInput);
            while ((entry = zipStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                name = entry.getName();
                if (name.startsWith(BINPATCH_PREFIX) && name.endsWith(BINPATCH_SUFFIX)) {
                	System.out.println("[ForgeAdapter] Adding binpatch "+name);
                    Binpatch binpatch = new Binpatch(zipStream);
                    //addClassFile(binpatch.obfClass);
                    patches.put(binpatch.filename, binpatch);
                }
            }
        } finally {
        	Utils.close(jar);
            Utils.close(zipStream);
            Utils.close(lzma);
            Utils.close(raw);
        }
    }

    public class Binpatch {
        final String filename;
        final String obfClass;
        final String deobfClass;
        final boolean hasInput;
        final int checksum;
        final byte[] patchData;

        Binpatch(InputStream input) throws IOException {
            DataInputStream data = new DataInputStream(input);
            filename = data.readUTF() + ".class";
            obfClass = data.readUTF();
            deobfClass = data.readUTF();
            hasInput = data.readBoolean();
            checksum = hasInput ? data.readInt() : 0;
            int expLen = data.readInt();
            patchData = new byte[expLen];
            int actualLen = 0;
            while (actualLen < expLen) {
                int count = data.read(patchData, actualLen, expLen - actualLen);
                if (count <= 0) {
                    break;
                }
                actualLen += count;
            }
            if (actualLen != expLen) {
                throw new IOException(String.format("Patch %s: EOF at %d/%d bytes", filename, actualLen, expLen));
            }
        }
        
        public String getObfClass() { return obfClass; }
        public String getDeobfClass() { return deobfClass; }

        public byte[] getInput(JarFile jar) throws IOException {
            if (hasInput) {
                JarEntry entry = jar.getJarEntry(filename);
                if (entry == null) {
                    throw new IOException(String.format("Patch %s: input file is missing", filename));
                }
                InputStream inputStream = jar.getInputStream(entry);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Utils.copyStream(inputStream, outputStream);
                Utils.close(inputStream);
                Utils.close(outputStream);
                byte[] output = outputStream.toByteArray();
                Adler32 adler = new Adler32();
                adler.update(output);
                int actual = (int) adler.getValue();
                if (actual != checksum) {
                    throw new IOException(String.format("Patch %s: invalid checksum: expected=%08x, actual=%08x", filename, checksum, actual));
                }
                return output;
            } else {
                return new byte[0];
            }
        }

        public byte[] apply(byte[] input) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayInputStream diff = new ByteArrayInputStream(patchData);
            try {
                if (gdiffConstructor1 != null) {
                    Object patcher = gdiffConstructor1.newInstance();
                    gdiffPatchMethod.invoke(patcher, input, diff, output);
                } else {
                    gdiffConstructor2.newInstance(input, diff, output);
                }
                return output.toByteArray();
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
