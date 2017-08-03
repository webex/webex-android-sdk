package com.cisco.spark.android.locus.requests;

import android.net.*;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.mercury.events.DeclineReason;

public class DeclineLocusRequest extends DeltaRequest {
    private Uri deviceUrl;
    private DeclineReason reason;

    public DeclineLocusRequest(CoreFeatures coreFeatures, Uri deviceUrl, DeclineReason reason) {
        super(coreFeatures);
        this.deviceUrl = deviceUrl;
        this.reason = reason;
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public DeclineReason getReason() {
        return reason;
    }
}
