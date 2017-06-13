package com.cisco.spark.android.locus.events;

/**
 * Error joining locus
 */
public class ErrorJoiningLocusEvent {
    private String errorMessage;
    private int errorCode;

    public ErrorJoiningLocusEvent(String errorMessage, int errorCode) {
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
