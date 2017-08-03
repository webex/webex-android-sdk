package com.cisco.spark.android.roap.model;


public class RoapExternalSessionException extends Exception {
    private RoapErrorType errorType;

    public RoapExternalSessionException(RoapErrorType errorType, String msg) {
        super(msg);
        this.errorType = errorType;
    }

    public RoapErrorType getErrorType() {
        return errorType;
    }
}
