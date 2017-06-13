package com.cisco.spark.android.metrics.value;


import android.net.Uri;

import com.cisco.spark.android.callcontrol.CallInitiationOrigin;

public class CallToggleAudioOnlyMetricValue {
    private String locusId;
    private Uri deviceUrl;
    private String locusTimestamp;
    private String requestTimestamp;

    private long callDurationMillis;
    private String networkType;
    private String callOrigin;
    private String initialMediaType = "video"; // calls always start with video for now in Android client


    public CallToggleAudioOnlyMetricValue(String locusId, Uri deviceUrl, String requestTimestamp, String locusTimestamp,
                                          CallInitiationOrigin callInitiationOrigin, long callDurationMillis, String networkType) {
        this.locusId = locusId;
        this.deviceUrl = deviceUrl;
        this.requestTimestamp = requestTimestamp;
        this.locusTimestamp = locusTimestamp;
        this.callDurationMillis = callDurationMillis;
        this.networkType = networkType;

        if (callInitiationOrigin != null) {
            this.callOrigin = callInitiationOrigin.getValue();
        } else {
            this.callOrigin = CallInitiationOrigin.CallOriginationUnknown.getValue();
        }

    }


}
