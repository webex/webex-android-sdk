package com.cisco.spark.android.client;

import com.cisco.spark.android.calliope.CalliopeClusterResponse;
import com.cisco.spark.android.status.HealthCheckResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import rx.Observable;

public interface CalliopeClient {

    @GET("clusters")
    Call<CalliopeClusterResponse> getClusters();

    @GET("ping")
    Observable<HealthCheckResponse> ping();
}
