package com.cisco.spark.android.locus.requests;

import android.net.Uri;


public class GetSipUriRequest {
    private Uri deviceUrl;

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }
}
