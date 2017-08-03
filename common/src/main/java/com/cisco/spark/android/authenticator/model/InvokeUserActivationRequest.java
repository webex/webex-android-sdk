package com.cisco.spark.android.authenticator.model;

public class InvokeUserActivationRequest {
    private String verificationToken;
    private String scope;

    public InvokeUserActivationRequest(String verificationToken, String scope) {
        this.verificationToken = verificationToken;
        this.scope = scope;
    }
}
