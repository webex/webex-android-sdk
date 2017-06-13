package com.cisco.spark.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import com.github.benoitdion.ln.Ln;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Properties;

import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.Cache;

public class FileUtils {

    public static File mkdir(String dirName) {
        File logDir = new File(dirName);
        if (!logDir.exists()) {
            if (!logDir.mkdirs())
                Ln.e("Failed to make directory");
        }
        return logDir;
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            if (source != null) {
                destination.transferFrom(source, 0, source.size());
            }
        } finally {
            close(source);
            close(destination);
        }
    }

    public static long getFilesystemSize(File fileOrDir) {
        long ret = 0;
        try {
            if (fileOrDir != null && fileOrDir.exists()) {
                if (fileOrDir.isFile()) {
                    ret = fileOrDir.length();
                } else if (fileOrDir.isDirectory()) {
                    for (File file : fileOrDir.listFiles()) {
                        ret += getFilesystemSize(file);
                    }
                }
            }
        } catch (Exception e) {
            Ln.e(e);
        }
        return ret;
    }

    public static void writeFileFromRaw(Context context, int resId, File outfile) {
        if (outfile == null || context == null) {
            Ln.e("Invalid Arg passed to writeFileFromRaw");
            return;
        }

        InputStream is = context.getResources().openRawResource(resId);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outfile);
            streamCopy(is, fos);
            is = null;
        } catch (IOException e) {
            Ln.e(e, "Failed writing raw resource to file " + outfile.getAbsolutePath());
        } finally {
            close(is);
        }
    }

    public static String readFile(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            Ln.e(e, "Error reading file: " + file.getAbsolutePath());
            return null;
        }
    }

    public static void streamCopy(InputStream in, OutputStream out) {
        final int bufferLen = 1024 * 10;

        byte[] buffer = new byte[bufferLen];
        int read;

        try {
            while ((read = in.read(buffer, 0, bufferLen)) >= 0) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            Ln.e(e, "Failed copying stream");
        } finally {
            close(in);
            close(out);
        }
    }

    public static File getCacheDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null)
            cacheDir = context.getCacheDir();
        return cacheDir;
    }

    public static boolean externalStorageFilePresent(String fileName) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File directory = Environment.getExternalStorageDirectory();
            return new File(directory, fileName).exists();
        }
        return false;
    }

    /*
     * read properties from external file
     */
    public static Properties readPropertiesFileFromExternalStorage(String fileName) {
        Properties properties = new Properties();

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File directory = Environment.getExternalStorageDirectory();
            File file = new File(directory + "/" + fileName);
            if (file.exists()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(file));
                    properties.load(reader);
                } catch (Exception e) {
                    Ln.d(e);
                } finally {
                    close(reader);
                }
            }
        }
        return properties;
    }

    /**
     * Close anything closeable.  Null OK.
     *
     * @param closeable anything closeable
     */
    public static void close(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (IOException ignore) {
        }
    }

    public static File getTmpFile(File tmpDir, String name, String ext) {
        tmpDir.mkdirs();
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(name, ext, tmpDir);
        } catch (IOException ex) {
            Ln.e(ex, "Error creating temp file");
        }
        return tmpFile;
    }

    @SuppressLint("NewApi")
    public static String getContentFileName(Context providedContext, Cache cacheType, Uri uri, String filename) {
        if (TextUtils.isEmpty(filename) && "content".equals(uri.getScheme())) {
            String displayNameColumn;

            if (UIUtils.hasKitKat() && DocumentsContract.isDocumentUri(providedContext, uri)) {
                displayNameColumn = DocumentsContract.Document.COLUMN_DISPLAY_NAME;
            } else if (MediaStore.AUTHORITY.equals(uri.getAuthority())) {
                displayNameColumn = MediaStore.MediaColumns.DISPLAY_NAME;
            } else {
                displayNameColumn = OpenableColumns.DISPLAY_NAME;
            }

            Cursor c = null;
            try {
                String[] columns = {displayNameColumn};
                c = providedContext.getContentResolver().query(uri, columns, null, null, null);
                if (c == null) // content unavailable
                    return null;
                if (c.moveToNext())
                    filename = c.getString(c.getColumnIndex(displayNameColumn));

                // Attempt identify ambiguous files; or log data for troubleshooting
                if (MimeUtils.getExtension(filename).isEmpty()) {
                    String mimeType = providedContext.getContentResolver().getType(uri);
                    String extension = MimeUtils.getExtensionByMimeType(mimeType);
                    if (!extension.isEmpty()) {
                        filename = filename.trim() + "." + extension;
                    } else {
                        Ln.d("Unable to classify selected file=%s, mimeType=%s, uri=%s", filename, mimeType, uri.toString());
                    }
                }
            } catch (Exception e) {
                Ln.e(e, "Caught exception ");
            } finally {
                close(c);
            }
        }

        if (TextUtils.isEmpty(filename)) {
            filename = uri.getLastPathSegment();
        }

        if (cacheType == Cache.THUMBNAIL && MimeUtils.getContentTypeByFilename(filename) != MimeUtils.ContentType.IMAGE) {
            filename += ".png";
        }
        return filename;
    }

    public static String appendBaseFilename(String originalFilename, int i) {
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0)
            dot = originalFilename.length();

        return new StringBuilder(originalFilename)
                .insert(dot, i)
                .toString();
    }

    public static byte[] fileToByteArray(File file) {
        try {
            long fileSize = file.length();
            if (fileSize > Integer.MAX_VALUE) {
                Ln.w("file too big to transfer to byte[]");
                return null;
            }
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) fileSize];
            int offset = 0;
            int numRead = fis.read(buffer, offset, buffer.length - offset);
            while (offset < buffer.length && numRead >= 0) {
                offset += numRead;
            }
            if (offset != buffer.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
            fis.close();
            return buffer;
        } catch (IOException e) {
            Ln.e(e, "Error reading file: " + file.getAbsolutePath());
            return null;
        }
    }
}
