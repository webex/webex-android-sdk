package com.cisco.spark.android.client;

import com.cisco.spark.android.locus.model.UserRecentSessions;
import com.cisco.spark.android.status.HealthCheckResponse;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface JanusClient {
    @GET("history/userSessions")
    Call<UserRecentSessions> getRecentCalls(@Query("from") String from, @Query("limit") int limit, @Query("sort") String sort);

    @GET("ping")
    Observable<Response<HealthCheckResponse>> ping();
}
