package com.cisco.spark.android.model;

import com.cisco.spark.android.authenticator.OAuth2Tokens;

public class UserSession {
    private User user;
    private OAuth2Tokens token;

    public UserSession(User user, OAuth2Tokens token) {
        this.user = user;
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public OAuth2Tokens getTokens() {
        return token;
    }
}
