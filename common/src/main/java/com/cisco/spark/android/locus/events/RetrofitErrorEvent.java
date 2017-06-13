package com.cisco.spark.android.locus.events;


public class RetrofitErrorEvent {
    private String errorMessage;
    private int errorCode;
    private Throwable throwable;

    public RetrofitErrorEvent(Throwable throwable) {
        this.throwable = throwable;
    }

    public RetrofitErrorEvent(String errorMessage, int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.throwable = null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
