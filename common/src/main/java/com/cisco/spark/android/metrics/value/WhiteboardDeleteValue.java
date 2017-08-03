package com.cisco.spark.android.metrics.value;

public class WhiteboardDeleteValue {
    private String errorType;
    private boolean isSuccessful;
    private int httpStatusCode;
    private String errorMsg;
    private String displayError;

    public WhiteboardDeleteValue(String errorType, boolean isSuccessful, int httpStatusCode, String errorMsg) {
        this.isSuccessful = isSuccessful;
        this.httpStatusCode = httpStatusCode;
        this.errorMsg = errorMsg;
        this.displayError = String.format("%s_%s", errorType, httpStatusCode);
    }

    @Override
    public String toString() {
        return "WhiteboardDeleteValue{" +
                "isSuccessful='" + isSuccessful + '\'' +
                ", httpStatusCode='" + httpStatusCode + '\'' +
                ", errorMsg='" + errorMsg +
                '}';
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
