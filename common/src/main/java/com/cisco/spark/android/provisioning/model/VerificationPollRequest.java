package com.cisco.spark.android.provisioning.model;

public class VerificationPollRequest {
    private String email;
    private String deviceId;
    private String messageId;

    public VerificationPollRequest(String email, String deviceId, String messageId) {
        this.email = email;
        this.deviceId = deviceId;
        this.messageId = messageId;
    }

    public String getEmail() {
        return email;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getMessageId() {
        return messageId;
    }
}
