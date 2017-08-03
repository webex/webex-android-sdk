package com.cisco.spark.android.provisioning.model;

public class UserActivationRequest {
    private String email;
    private String reqId;

    public UserActivationRequest(String email, String reqId) {
        this.email = email;
        this.reqId = reqId;
    }

    public String getEmail() {
        return email;
    }

    public String getReqId() {
        return reqId;
    }
}
