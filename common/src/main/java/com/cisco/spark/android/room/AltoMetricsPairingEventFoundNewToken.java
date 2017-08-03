package com.cisco.spark.android.room;

import com.cisco.spark.android.room.audiopairing.UltrasoundMetrics;

public class AltoMetricsPairingEventFoundNewToken {
    public UltrasoundMetrics getMetrics() {
        return metrics;
    }

    private UltrasoundMetrics metrics;

    public AltoMetricsPairingEventFoundNewToken(UltrasoundMetrics metrics) {
        this.metrics = metrics;
    }
}
