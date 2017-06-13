package com.cisco.spark.android.metrics.value;

import android.net.Uri;

public class AudioControlMetricValue {
    private Uri deviceUrl;
    private long durationTime;
    private String roomId;

    public AudioControlMetricValue(Uri deviceUrl, long durationTime, String roomId) {
        this.deviceUrl = deviceUrl;
        this.durationTime = durationTime;
        this.roomId = roomId;
    }
}
