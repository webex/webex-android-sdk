package com.cisco.spark.android.locus.events;

public class ConflictErrorJoiningLocusEvent {
    private String errorMessage;
    private int errorCode;

    public ConflictErrorJoiningLocusEvent(String errorMessage, int errorCode) {
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
