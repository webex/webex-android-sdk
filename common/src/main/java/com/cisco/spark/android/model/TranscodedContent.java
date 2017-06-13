package com.cisco.spark.android.model;

import android.net.Uri;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.sync.DisplayableFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class TranscodedContent {
    private String transcodedContentType;
    private ItemCollection<File> files = new ItemCollection<File>();
    private Uri uri;
    private String scr;

    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }

    public void setSecureContentReference(SecureContentReference secureContentReference) {
        this.secureContentReference = secureContentReference;
    }

    public String getScr() {
        return scr;
    }

    public void setScr(String scr) {
        this.scr = scr;
    }

    private SecureContentReference secureContentReference;

    public TranscodedContent(String transcodedContentType) {
        this.transcodedContentType = transcodedContentType;
    }

    public String getTranscodedContentType() {
        return transcodedContentType;
    }

    public void setFiles(ItemCollection<File> files) {
        this.files = files;
    }

    public ItemCollection<File> getFiles() {
        return files;
    }

    public List<DisplayableFile.Page> getPages() {
        List<DisplayableFile.Page> pages = new ArrayList<DisplayableFile.Page>();

        if ("BOX_VIEW".equals(transcodedContentType)) {
            for (int i = 0; i < files.getItems().size(); i++) {
                File file = files.getItems().get(i);
                if (isPage(file)) {
                    DisplayableFile.Page page = DisplayableFile.Page.from(file);
                    pages.add(page);
                }
            }
        }
        return pages;
    }

    private boolean isPage(File file) {
        return file.getDisplayName().toLowerCase(Locale.getDefault()).endsWith(".png");
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void decrypt(KeyObject key) throws IOException, ParseException {
        if (files != null) {
            for (File file : files.getItems()) {
                file.decrypt(key);
            }
        }
    }
}
