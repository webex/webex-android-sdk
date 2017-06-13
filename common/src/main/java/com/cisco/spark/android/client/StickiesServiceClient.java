package com.cisco.spark.android.client;

import com.cisco.spark.android.stickies.StickyPack;

import retrofit.http.GET;

public interface StickiesServiceClient {

    @GET("/pack")
    StickyPack getPackForUser();
}
