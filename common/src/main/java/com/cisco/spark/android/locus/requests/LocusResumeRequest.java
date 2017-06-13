package com.cisco.spark.android.locus.requests;

import android.net.Uri;

public class LocusResumeRequest {
    private final Uri deviceUrl;

    public LocusResumeRequest(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

}
