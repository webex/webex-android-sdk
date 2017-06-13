package com.cisco.spark.android.locus.events;


public class HighVolumeErrorJoiningLocusEvent {
    private String errorMessage;
    private int errorCode;

    public HighVolumeErrorJoiningLocusEvent(String errorMessage, int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
