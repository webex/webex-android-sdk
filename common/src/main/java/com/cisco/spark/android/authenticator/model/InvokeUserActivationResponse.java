package com.cisco.spark.android.authenticator.model;

import com.cisco.spark.android.authenticator.OAuth2Tokens;

public class InvokeUserActivationResponse {
    private String cookieValue;
    private String cookieName;
    private String cookieDomain;
    private String url;
    private OAuth2Tokens tokenData;

    public String getCookieValue() {
        return cookieValue;
    }

    public String getCookieName() {
        return cookieName;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public String getUrl() {
        return url;
    }

    public OAuth2Tokens getTokenData() {
        return tokenData;
    }
}
