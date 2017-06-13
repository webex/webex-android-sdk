package com.cisco.spark.android.client;


import com.cisco.spark.android.presence.PresenceEvent;
import com.cisco.spark.android.presence.PresenceEventResponse;
import com.cisco.spark.android.presence.PresenceStatusList;
import com.cisco.spark.android.presence.PresenceStatusRequest;
import com.cisco.spark.android.presence.PresenceStatusResponse;
import com.cisco.spark.android.presence.PresenceSubscriptionList;
import com.cisco.spark.android.presence.PresenceSubscriptionResponse;
import com.cisco.spark.android.presence.PresenceUserEvents;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PresenceServiceClient {

    @POST("subscriptions")
    Call<PresenceSubscriptionResponse> postSubscribeTo(@Body PresenceStatusRequest request);

    @GET("subscriptions")
    Call<PresenceSubscriptionList> getSubscriptionList();

    @POST("events")
    Call<PresenceEventResponse> postPresenceEvent(@Body PresenceEvent event);

    @GET("events")
    Call<PresenceUserEvents> getUserPresence(@Query("userId") String userId);

    @GET("compositions")
    Call<PresenceStatusResponse> getUserComposition(@Query("userId") String userId);

    @POST("compositions")
    Call<PresenceStatusList> getUserCompositions(@Body PresenceStatusRequest request);
}
