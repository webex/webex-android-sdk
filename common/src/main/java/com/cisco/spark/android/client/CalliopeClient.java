package com.cisco.spark.android.client;

import com.cisco.spark.android.calliope.CalliopeClusterResponse;

import retrofit.http.GET;

public interface CalliopeClient {

    @GET("/clusters")
    CalliopeClusterResponse getClusters();
}
