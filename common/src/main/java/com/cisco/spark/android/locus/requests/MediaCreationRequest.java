package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.locus.model.MediaConnection;


public class MediaCreationRequest {
    private Uri deviceUrl;
    private MediaConnection localMedia;

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public void setLocalMedia(MediaConnection localMedia) {
        this.localMedia = localMedia;
    }
}
