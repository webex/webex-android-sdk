package com.cisco.spark.android.util;

import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.cisco.spark.android.R;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.MediaType;

public class MimeUtils {
    public static boolean isImageExt(String extension) {
        return CONTENT_TYPE_BY_EXTENSION.get(extension) == ContentType.IMAGE;
    }

    public enum ContentType {
        IMAGE(R.drawable.ic_file_image),
        EXCEL(R.drawable.ic_file_excel),
        POWERPOINT(R.drawable.ic_file_powerpoint),
        WORD(R.drawable.ic_file_word),
        PDF(R.drawable.ic_file_pdf),
        VIDEO(R.drawable.ic_file_video),
        AUDIO(R.drawable.ic_file_audio),
        ZIP(R.drawable.ic_file_zip),
        UNKNOWN(R.drawable.ic_file_unknown),
        SKETCH(R.drawable.ic_file_sketch);

        public int drawableResource;

        ContentType(int rc) {
            drawableResource = rc;
        }

        public boolean shouldTranscode() {
            switch (this) {
                case POWERPOINT:
                case WORD:
                case PDF:
                    return true;
            }
            return false;
        }
    }

    private static final HashMap<String, ContentType> CONTENT_TYPE_BY_EXTENSION;

    static {
        CONTENT_TYPE_BY_EXTENSION = new HashMap<>();
        populateDrawablesByExtMap(ContentType.EXCEL, new String[]{"xls", "xlsx", "xlsm", "xltx", "xltm"});
        populateDrawablesByExtMap(ContentType.POWERPOINT, new String[]{"ppt", "pptx", "pptm", "potx", "potm", "ppsx", "ppsm", "sldx", "sldm"});
        populateDrawablesByExtMap(ContentType.WORD, new String[]{"doc", "docx", "docm", "dotx", "dotm"});
        populateDrawablesByExtMap(ContentType.PDF, new String[]{"pdf"});
        populateDrawablesByExtMap(ContentType.VIDEO, new String[]{"mp4", "m4p", "mpg", "mpeg", "3gp", "3g2", "mov", "avi", "wmv", "qt", "m4v", "flv", "m4v"});
        populateDrawablesByExtMap(ContentType.AUDIO, new String[]{"mp3", "wav", "wma"});
        populateDrawablesByExtMap(ContentType.IMAGE, new String[]{"jpg", "jpeg", "png", "gif"});
        populateDrawablesByExtMap(ContentType.ZIP, new String[]{"zip"});
        populateDrawablesByExtMap(ContentType.SKETCH, new String[]{"sketch"});
    }

    private static void populateDrawablesByExtMap(ContentType type, String[] extensions) {
        for (String ext : extensions) {
            CONTENT_TYPE_BY_EXTENSION.put(ext, type);
        }
    }

    public static String getMimeType(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }

        String ext = getExtension(path);
        if (!TextUtils.isEmpty(ext)) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
        } else {
            return "*/*";
        }
    }

    public static MediaType getMediaType(String path) {
        return MediaType.parse(getMimeType(path));
    }

    public static int getDefaultDrawableByFilename(String filename) {
        return getContentTypeByFilename(filename).drawableResource;
    }

    public static ContentType getContentTypeByFilename(String filename) {
        if (TextUtils.isEmpty(filename))
            return ContentType.UNKNOWN;

        ContentType ret = CONTENT_TYPE_BY_EXTENSION.get(getExtension(filename.toLowerCase(Locale.US)));
        if (ret == null)
            return ContentType.UNKNOWN;
        return ret;
    }

    public static String getExtensionByMimeType(String mimeType) {
        if (mimeType.isEmpty() || "*/*".equals(mimeType)) {
            return "";
        } else {
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }
    }

    public static String getExtension(String str) {
        if (str == null)
            return null;

        int i = str.lastIndexOf('.');
        if (i >= 0)
            return str.substring(i + 1);
        return "";
    }

    public static boolean isEmptyOrGeneric(String mimeType) {
        return TextUtils.isEmpty(mimeType) || "*/*".equals(mimeType);
    }

    public static String removeFileExtension(String filename) {
        if (filename == null)
            return null;

        int i = filename.lastIndexOf('.');
        if (i >= 0)
            return filename.substring(0, i);
        return filename;
    }
    /**
     * For files with no reliable extension. Currently only implemented for image types and video
     * types.
     *
     * @param file The file
     * @return The extension, not including a . character
     */
    public static String guessExtensionForUnknownFile(File file) {
        if (!file.exists())
            return "";

        try {
            // See if we can decode it as an image
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), op);
            if (op.outMimeType != null) {
                return MimeUtils.getExtensionByMimeType(op.outMimeType);
            }

            // Check the file header for common video file types
            byte[] bytes = new byte[12];
            Arrays.fill(bytes, (byte) 0);
            FileInputStream fin = new FileInputStream(file);
            fin.read(bytes, 0, 12);

            fin.close();

            String ftypHeader = new String(Arrays.copyOfRange(bytes, 4, 12));

            if (TextUtils.isEmpty(ftypHeader))
                return "";

            if ("ftypisom".equals(ftypHeader))
                return "mp4";

            if ("ftypMSNV".equals(ftypHeader))
                return "mp4";

            if ("ftypmp42".equals(ftypHeader))
                return "m4v";

            if (ftypHeader.startsWith("ftyp3gp"))
                return "mp4";

            if (ftypHeader.startsWith("ftypqt"))
                return "mov";

        } catch (Throwable e) {
            Ln.i(e, "Unknown file type");
        }

        return "";
    }
}
