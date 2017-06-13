package com.cisco.spark.android.util;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * This class is a utility helper to recursively compress the contents of a zipDirectory into a ZIP
 * file.
 */
public class ZipUtils {
    // Read and write in 8kb blocks
    private static final int BUFFER_SIZE = 8 * 1024;

    public static boolean zipDirectory(File directory, File zip) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
            zipRecursive(directory, directory, zos);
            zos.close();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    private static void zipRecursive(File directory, File base, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read = 0;

        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()) {
                zipRecursive(files[i], base, zos);
            } else {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1));
                entry.setTime(files[i].lastModified());
                zos.putNextEntry(entry);
                while (-1 != (read = in.read(buffer))) {
                    zos.write(buffer, 0, read);
                }
                in.close();
            }
        }
    }

    public static boolean unZip(File zip, File extractTo) {
        try {
            ZipFile archive = new ZipFile(zip);
            Enumeration entries = archive.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File file = new File(extractTo, entry.getName());
                if (entry.isDirectory() && !file.exists()) {
                    file.mkdirs();
                } else {
                    if (!file.getParentFile().exists()) {
                     file.getParentFile().mkdirs();
                 }
             }

             InputStream in = archive.getInputStream(entry);
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;

                while (-1 != (read = in.read(buffer))) {
                    out.write(buffer, 0, read);
                }

                in.close();
                out.close();
            }
            archive.close();
        } catch (IOException ex) {
            Log.d("ZipUtils", "Error unzipping " + zip.getPath(), ex);
            return false;
        }

        return true;
    }
}
