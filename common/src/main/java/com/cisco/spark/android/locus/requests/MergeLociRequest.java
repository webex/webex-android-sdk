package com.cisco.spark.android.locus.requests;


import android.net.Uri;


public class MergeLociRequest  {

    private Uri deviceUrl;
    private String locusId;

    public MergeLociRequest(Uri deviceUrl, String locusId) {
        this.deviceUrl = deviceUrl;
        this.locusId = locusId;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }


    public String getLocusId() {
        return locusId;
    }

    public void setLocusId(String locusId) {
        this.locusId = locusId;
    }
}
