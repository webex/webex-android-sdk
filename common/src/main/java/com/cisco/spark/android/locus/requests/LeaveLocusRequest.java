package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;

public class LeaveLocusRequest extends DeltaRequest {
    private Uri deviceUrl;
    private String usingResource;

    public LeaveLocusRequest(CoreFeatures coreFeatures) {
        super(coreFeatures);
    }

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
