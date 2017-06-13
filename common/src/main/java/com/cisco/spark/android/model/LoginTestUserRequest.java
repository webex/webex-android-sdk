package com.cisco.spark.android.model;

import android.util.Log;

import com.cisco.spark.android.authenticator.OAuth2;

public class LoginTestUserRequest {
    private String email;
    private String password;
    private String scopes = OAuth2.UBER_SCOPES;

    public LoginTestUserRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
