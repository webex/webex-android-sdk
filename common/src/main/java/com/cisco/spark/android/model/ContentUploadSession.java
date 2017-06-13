package com.cisco.spark.android.model;

import android.net.Uri;

public class ContentUploadSession {
    private Uri url;
    private Uri uploadUrl;
    private Uri finishUploadUrl;

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
