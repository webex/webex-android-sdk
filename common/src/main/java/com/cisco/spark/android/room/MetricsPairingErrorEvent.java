package com.cisco.spark.android.room;

public class MetricsPairingErrorEvent {

    private String result;

    public MetricsPairingErrorEvent(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
