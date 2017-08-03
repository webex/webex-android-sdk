package com.cisco.spark.android.locus.events;

public class WhiteboardUnshareErrorEvent extends RetrofitErrorEvent {

    private String errorType;

    public WhiteboardUnshareErrorEvent(String errorType, String retrofitErrorMessage, int errorCode) {
        super(retrofitErrorMessage, errorCode);
        this.errorType = errorType;
    }

    public WhiteboardUnshareErrorEvent(String errorType) {
        super(null);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}
