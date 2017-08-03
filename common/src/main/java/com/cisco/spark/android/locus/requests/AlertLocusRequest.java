package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;

public class AlertLocusRequest extends DeltaRequest {
    private Uri deviceUrl;

    public AlertLocusRequest(CoreFeatures coreFeatures) {
        super(coreFeatures);
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }
}
