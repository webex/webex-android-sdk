package com.cisco.spark.android.client;

import com.cisco.spark.android.metrics.model.GenericMetricsRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface MetricsPreloginClient {
    @POST("clientmetrics-prelogin")
    public Call<Void> postClientMetric(@Body GenericMetricsRequest request);
}
