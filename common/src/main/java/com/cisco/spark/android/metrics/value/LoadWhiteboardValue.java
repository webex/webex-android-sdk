package com.cisco.spark.android.metrics.value;


public class LoadWhiteboardValue {
    private long networkLoadTime;
    private long decryptLoadTime;
    private String whiteboardID;
    private int size;

    public LoadWhiteboardValue(long networkLoadTime, long decryptLoadTime, int size, String whiteboardID) {
        this.decryptLoadTime = decryptLoadTime;
        this.networkLoadTime = networkLoadTime;
        this.whiteboardID = whiteboardID;
        this.size = size;
    }
}
