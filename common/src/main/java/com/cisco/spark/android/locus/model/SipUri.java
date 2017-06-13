package com.cisco.spark.android.locus.model;


import android.net.Uri;

public class SipUri {
    private String uri;
    private Uri locusUrl;
    private String userId;
    private String numericCode;


    public String getUri() {
        return uri;
    }

    public Uri getLocusUrl() {
        return locusUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getNumericCode() {
        return numericCode;
    }
}
