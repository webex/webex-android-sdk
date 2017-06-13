package com.cisco.spark.android.provisioning.model;

public class EmailVerificationRequest {
    private String email;
    private String pushId;
    private String deviceId;
    private String deviceName;

    public EmailVerificationRequest(String email, String pushId, String deviceId, String deviceName) {
        this.email = email;
        this.pushId = pushId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    public String getEmail() {
        return email;
    }

    public String getPushId() {
        return pushId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }
}
