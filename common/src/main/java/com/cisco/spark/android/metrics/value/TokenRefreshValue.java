package com.cisco.spark.android.metrics.value;

public class TokenRefreshValue {

    private float ms;
    private String roomUser;
    private String roomUrl;

    public TokenRefreshValue(float ms, String roomUser, String roomUrl) {
        this.ms = ms;
        this.roomUser = roomUser;
        this.roomUrl = roomUrl;
    }

}
