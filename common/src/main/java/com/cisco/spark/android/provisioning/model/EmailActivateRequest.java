package com.cisco.spark.android.provisioning.model;

public class EmailActivateRequest {
    private String encryptedQueryString;

    public EmailActivateRequest(String encryptedQueryString) {
        this.encryptedQueryString = encryptedQueryString;
    }
}
