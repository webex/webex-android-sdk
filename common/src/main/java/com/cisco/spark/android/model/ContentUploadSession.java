package com.cisco.spark.android.model;

import android.net.Uri;

public class ContentUploadSession {

    private Uri url;
    private Uri uploadUrl;
    private Uri finishUploadUrl;

    public ContentUploadSession() {

    }

    public ContentUploadSession(Uri url, Uri uploadUrl, Uri finishUploadUrl) {
        this.url = url;
        this.uploadUrl = uploadUrl;
        this.finishUploadUrl = finishUploadUrl;
    }

    public Uri getUrl() {
        return this.url;
    }

    public Uri getUploadUrl() {
        return this.uploadUrl;
    }

    public Uri getFinishUploadUrl() {
        return this.finishUploadUrl;
    }
}
