package com.cisco.spark.android.client;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Url;


public interface LinusClient {
    // Used in integration tests only
    @GET
    Call<Void> triggerRoapOffer(@Header("Authorization") String authorization, @Header("TrackingID") String trackingId, @Url String url);
}
