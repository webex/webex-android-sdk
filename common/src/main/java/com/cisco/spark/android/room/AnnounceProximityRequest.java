package com.cisco.spark.android.room;


import android.net.Uri;

public class AnnounceProximityRequest {
    private String token;
    private Uri deviceUrl;

    public AnnounceProximityRequest(String token, Uri deviceUrl) {
        this.token = token;
        this.deviceUrl = deviceUrl;
    }

    public String getToken() {
        return token;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }
}
