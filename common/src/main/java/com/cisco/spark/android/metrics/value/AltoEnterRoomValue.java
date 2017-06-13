package com.cisco.spark.android.metrics.value;

public class AltoEnterRoomValue {

    private boolean success;

    private String roomUser;
    private String roomUrl;

    /**
     * Reusing this value for Enter and Re-enter
     * They are differed by key
     */
    public AltoEnterRoomValue(String roomUser, String roomUrl) {
        // Always true
        this.success = true;
        this.roomUser = roomUser;
        this.roomUrl = roomUrl;
    }
}
