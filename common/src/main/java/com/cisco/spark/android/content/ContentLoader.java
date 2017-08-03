package com.cisco.spark.android.content;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.IOException;

public class ContentLoader {
    private final static String CONTENT_DIR = "content";
    private String contentLocation;
    private MetricsReporter metricsReporter;

    public ContentLoader(Context context, MetricsReporter metricsReporter) {
        this.contentLocation = FileUtils.getCacheDir(context).getPath() + File.separator + CONTENT_DIR;
        this.metricsReporter = metricsReporter;
    }

    public String getContentDirectoryName() {
        return contentLocation;
    }

    public boolean addContent(File srcFile, String conversationId, Uri url) {
        if (srcFile == null || !srcFile.exists())
            return false;

        try {
            File dstFile = getContentFile(getConversationDir(conversationId), url, MimeUtils.getExtension(srcFile.getName()));
            if (!dstFile.exists())
                dstFile.createNewFile();

            FileUtils.copyFile(srcFile, dstFile);
        } catch (IOException e) {
            Ln.e(e, "Error adding content file");
            return false;
        }

        return true;
    }

    //
    // Common AsyncTask helper functions
    //
    private File getConversationDir(String conversationId) {
        File dir = new File(getContentDirectoryName() + File.separator + conversationId);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Ln.e("Error creating conversation directory (%s)", dir.getAbsolutePath());
                return null;
            }
        }
        return dir;
    }

    public File getContentFile(File conversationDir, Uri url, String ext) {

        if ("file".equals(url.getScheme())) {
            try {
                return new File(url.getPath());
            } catch (Exception e) {
                Ln.e("Invalid content file url", e);
                return null;
            }
        } else {
            String contentId = Strings.md5(url.toString());

            if (!TextUtils.isEmpty(ext) && ext.charAt(0) != '.')
                ext = "." + ext;

            return new File(conversationDir.getAbsolutePath() + File.separator + contentId + ext);
        }
    }
}
