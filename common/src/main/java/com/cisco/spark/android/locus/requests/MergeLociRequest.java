package com.cisco.spark.android.locus.requests;


import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;


public class MergeLociRequest extends DeltaRequest {

    private Uri deviceUrl;
    private String locusId;

    public MergeLociRequest(CoreFeatures coreFeatures, Uri deviceUrl, String locusId) {
        super(coreFeatures);
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
