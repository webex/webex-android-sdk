package com.cisco.spark.android.model;

import android.net.Uri;

public class ContentAction {

    public final static String TYPE_EDIT = "edit";
    public final static String TYPE_READONLY = "read-only";
    public final static String TYPE_VIDEOSTREAM = "video-stream";

    private String type;
    private String mimeType;
    private Uri url;

    public ContentAction(String type, String mimeType, Uri url) {
        this.type = type;
        this.mimeType = mimeType;
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Uri getUrl() {
        return url;
    }

    public void setUrl(Uri url) {
        this.url = url;
    }
}
