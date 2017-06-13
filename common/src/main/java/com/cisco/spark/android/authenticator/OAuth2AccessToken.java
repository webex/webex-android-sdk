package com.cisco.spark.android.authenticator;

import android.text.TextUtils;

import com.github.benoitdion.ln.Ln;
import com.google.gson.annotations.SerializedName;

public class OAuth2AccessToken {

    @SerializedName("expires_in")
    protected long expiresIn;

    @SerializedName("access_token")
    protected String accessToken;

    protected long optimisticRefreshTime;
    protected String scopes;

    public OAuth2AccessToken() {

    }

    // Exposed for testing
    public OAuth2AccessToken(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getScopes() {
        if (TextUtils.isEmpty(scopes))
            return OAuth2.UBER_SCOPES;
        return scopes;
    }

    public String getAccessToken() {
        return accessToken;
    }

    // Exposed for testing.
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void recordOptimisticRefreshTime() {
        // If the token is valid for more than an hour, refresh it after an hour is up, so that we have
        // a buffer in case there's a CI outage.

        final long minutes = 60; // 60 seconds in a minute -- expiresIn is expressed in seconds
        final long refreshIn = expiresIn < (60 * minutes) ? expiresIn - (5 * minutes) : (60 * minutes);
        optimisticRefreshTime = System.currentTimeMillis() + refreshIn * 1000L;
        Ln.i("New token expires in " + expiresIn + " seconds. Will refresh in " + refreshIn);
    }

    public String getAuthorizationHeader() {
        return "Bearer " + getAccessToken();
    }

    public boolean shouldRefreshNow() {
        if (optimisticRefreshTime == 0)
            recordOptimisticRefreshTime();

        return System.currentTimeMillis() > optimisticRefreshTime;
    }
}
