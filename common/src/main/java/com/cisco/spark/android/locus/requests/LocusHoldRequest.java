package com.cisco.spark.android.locus.requests;

import android.net.Uri;

public class LocusHoldRequest {
    private final Uri deviceUrl;

    public LocusHoldRequest(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }
}
