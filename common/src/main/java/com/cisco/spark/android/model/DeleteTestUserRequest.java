package com.cisco.spark.android.model;

import com.google.gson.annotations.*;

public class DeleteTestUserRequest {
    @SerializedName("user_id")
    private final String userId;
    @SerializedName("refresh_token")
    private final String refreshToken;

    public DeleteTestUserRequest(String userId, String refreshToken) {
        this.userId = userId;
        this.refreshToken = refreshToken;
    }
}
