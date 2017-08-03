package com.cisco.spark.android.client;

import com.cisco.spark.android.lyra.model.AdvertisementByToken;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface LyraProximityServiceClient {

    /**
     * Announce that we are in proximity of a room
     * @param token token and deviceUrl
     */
    @GET("ultrasound/advertisements")
    Call<AdvertisementByToken> announceProximity(@Query("token") String token);

}
