package com.cisco.spark.android.provisioning.model;

public class ProvisioningErrorResponse {
    private Integer errorCode;
    private String message;
    private String trackingId;

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setMessage(String messsage) {
        this.message = messsage;
    }

    public String getMessage() {
        return message;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getTrackingId() {
        return trackingId;
    }
}
