package com.cisco.spark.android.provisioning.model;

public class PollingEmailStatus {
    private String smtpCode;
    private String smtpMessage;
    private String messageStatus;
    private String description;

    public String getSmtpCode() {
        return smtpCode;
    }

    public String getSmtpMessage() {
        return smtpMessage;
    }

    public String getMessageStatus() {
        return messageStatus;
    }

    public String getDescription() {
        return description;
    }
}
