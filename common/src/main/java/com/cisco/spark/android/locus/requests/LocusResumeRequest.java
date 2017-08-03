package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;

public class LocusResumeRequest extends DeltaRequest {
    private final Uri deviceUrl;

    public LocusResumeRequest(CoreFeatures coreFeatures, Uri deviceUrl) {
        super(coreFeatures);
        this.deviceUrl = deviceUrl;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

}
