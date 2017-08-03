package com.cisco.spark.android.locus.events;

public class WhiteboardShareErrorEvent extends RetrofitErrorEvent {

    private String errorType;

    public WhiteboardShareErrorEvent(String errorType, String retrofitErrorMessage, int errorCode) {
        super(retrofitErrorMessage, errorCode);
        this.errorType = errorType;
    }

    public WhiteboardShareErrorEvent(String errorType) {
        super(null);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}
