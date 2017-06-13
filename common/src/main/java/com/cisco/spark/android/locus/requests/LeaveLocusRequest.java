package com.cisco.spark.android.locus.requests;

import android.net.Uri;

public class LeaveLocusRequest {
    private Uri deviceUrl;
    private String usingResource;

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public String getUsingResource() {
        return usingResource;
    }

    public void setUsingResource(String usingResource) {
        this.usingResource = usingResource;
    }
}
