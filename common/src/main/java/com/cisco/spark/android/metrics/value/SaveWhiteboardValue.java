package com.cisco.spark.android.metrics.value;

public class SaveWhiteboardValue {
    private long savetime;
    private String whiteboardID;

    public SaveWhiteboardValue(long savetime, String whiteboardID) {
        this.savetime = savetime;
        this.whiteboardID = whiteboardID;
    }
}
