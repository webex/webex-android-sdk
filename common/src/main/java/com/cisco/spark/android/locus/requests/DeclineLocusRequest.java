package com.cisco.spark.android.locus.requests;

import android.net.*;
import com.cisco.spark.android.mercury.events.DeclineReason;

public class DeclineLocusRequest {
    private Uri deviceUrl;
    private DeclineReason reason;

    public DeclineLocusRequest(Uri deviceUrl, DeclineReason reason) {
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
