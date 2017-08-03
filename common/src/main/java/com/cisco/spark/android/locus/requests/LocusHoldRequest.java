package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;

public class LocusHoldRequest extends DeltaRequest {
    private final Uri deviceUrl;

    public LocusHoldRequest(CoreFeatures coreFeatures, Uri deviceUrl) {
        super(coreFeatures);
        this.deviceUrl = deviceUrl;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }
}
