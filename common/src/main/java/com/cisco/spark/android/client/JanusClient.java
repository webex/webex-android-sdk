package com.cisco.spark.android.client;

import com.cisco.spark.android.locus.model.UserRecentSessions;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface JanusClient {
    @GET("history/userSessions")
    Call<UserRecentSessions> getRecentCalls(@Query("from") String from, @Query("limit") int limit, @Query("sort") String sort);
}
