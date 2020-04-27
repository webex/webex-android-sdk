/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.utils;

import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.MediaType;

public class MimeUtils {
    private static final HashMap<String, ContentType> CONTENT_TYPE_BY_EXTENSION = new HashMap<>();

    public MimeUtils() {
    }

    public static boolean isImageExt(String extension) {
        return CONTENT_TYPE_BY_EXTENSION.get(extension) == MimeUtils.ContentType.IMAGE;
    }

    private static void populateDrawablesByExtMap(MimeUtils.ContentType type, String[] extensions) {
        for (String ext : extensions) {
            CONTENT_TYPE_BY_EXTENSION.put(ext, type);
        }
    }

    public static String getMimeType(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        } else {
            String ext = getExtension(path);
            return !TextUtils.isEmpty(ext) ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US)) : "*/*";
        }
    }

    public static MediaType getMediaType(String path) {
        return MediaType.parse(getMimeType(path));
    }

    public static MimeUtils.ContentType getContentTypeByFilename(String filename) {
        if (TextUtils.isEmpty(filename)) {
            return MimeUtils.ContentType.UNKNOWN;
        } else {
            MimeUtils.ContentType ret = CONTENT_TYPE_BY_EXTENSION.get(getExtension(filename.toLowerCase(Locale.US)));
            return ret == null ? MimeUtils.ContentType.UNKNOWN : ret;
        }
    }

    public static MimeUtils.ContentType getContentTypeByMimeType(String mimeType) {
        if (isEmptyOrGeneric(mimeType)) {
            return ContentType.UNKNOWN;
        } else {
            MimeUtils.ContentType ret = CONTENT_TYPE_BY_EXTENSION.get(getExtensionByMimeType(mimeType));
            return ret == null ? ContentType.UNKNOWN : ret;
        }
    }

    public static String getExtensionByMimeType(String mimeType) {
        return !mimeType.isEmpty() && !"*/*".equals(mimeType) ? MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) : "";
    }

    public static String getExtension(String str) {
        if (str == null) {
            return null;
        } else {
            int i = str.lastIndexOf(46);
            return i >= 0 ? str.substring(i + 1) : "";
        }
    }

    public static boolean isEmptyOrGeneric(String mimeType) {
        return TextUtils.isEmpty(mimeType) || "*/*".equals(mimeType);
    }

    public static String removeFileExtension(String filename) {
        if (filename == null) {
            return null;
        } else {
            int i = filename.lastIndexOf(46);
            return i >= 0 ? filename.substring(0, i) : filename;
        }
    }

    public static String guessExtensionForUnknownFile(File file) {
        if (file.exists()) {
            try {
                Options op = new Options();
                op.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), op);
                if (op.outMimeType != null) {
                    return getExtensionByMimeType(op.outMimeType);
                }

                byte[] bytes = new byte[12];
                Arrays.fill(bytes, (byte) 0);
                FileInputStream fin = new FileInputStream(file);
                fin.read(bytes, 0, 12);
                fin.close();
                String ftypHeader = new String(Arrays.copyOfRange(bytes, 4, 12));
                if (TextUtils.isEmpty(ftypHeader)) {
                    return "";
                }

                if ("ftypisom".equals(ftypHeader)) {
                    return "mp4";
                }

                if ("ftypMSNV".equals(ftypHeader)) {
                    return "mp4";
                }

                if ("ftypmp42".equals(ftypHeader)) {
                    return "m4v";
                }

                if (ftypHeader.startsWith("ftyp3gp")) {
                    return "mp4";
                }

                if (ftypHeader.startsWith("ftypqt")) {
                    return "mov";
                }
            } catch (Throwable var5) {
                Ln.i(var5, "Unknown file type");
            }
        }
        return "";
    }

    static {
        populateDrawablesByExtMap(MimeUtils.ContentType.EXCEL, new String[]{"xls", "xlsx", "xlsm", "xltx", "xltm"});
        populateDrawablesByExtMap(MimeUtils.ContentType.POWERPOINT, new String[]{"ppt", "pptx", "pptm", "potx", "potm", "ppsx", "ppsm", "sldx", "sldm"});
        populateDrawablesByExtMap(MimeUtils.ContentType.WORD, new String[]{"doc", "docx", "docm", "dotx", "dotm"});
        populateDrawablesByExtMap(MimeUtils.ContentType.PDF, new String[]{"pdf"});
        populateDrawablesByExtMap(MimeUtils.ContentType.VIDEO, new String[]{"mp4", "m4p", "mpg", "mpeg", "3gp", "3g2", "mov", "avi", "wmv", "qt", "m4v", "flv", "m4v"});
        populateDrawablesByExtMap(MimeUtils.ContentType.AUDIO, new String[]{"mp3", "wav", "wma"});
        populateDrawablesByExtMap(MimeUtils.ContentType.IMAGE, new String[]{"jpg", "jpeg", "png", "gif"});
        populateDrawablesByExtMap(MimeUtils.ContentType.ZIP, new String[]{"zip"});
        populateDrawablesByExtMap(MimeUtils.ContentType.SKETCH, new String[]{"sketch"});
    }

    public enum ContentType {
        IMAGE,
        EXCEL,
        POWERPOINT,
        WORD,
        PDF,
        VIDEO,
        AUDIO,
        ZIP,
        UNKNOWN,
        SKETCH;

        ContentType() {
        }

        public boolean shouldTranscode() {
            switch (this) {
                case POWERPOINT:
                case WORD:
                case PDF:
                case EXCEL:
                    return true;
                default:
                    return false;
            }
        }
    }
}