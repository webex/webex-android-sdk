package com.cisco.spark.android.client;

import com.cisco.spark.android.metrics.model.GenericMetricsRequest;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MetricsClient {
    @POST("metrics")
    public Call<Void> postGenericMetric(@Body MetricsReportRequest request);

    @POST("clientmetrics")
    public Call<Void> postClientMetric(@Body GenericMetricsRequest request);

    @POST("clientmetrics?alias=true")
    public Call<Void> postUserAlias(@Header("X-Prelogin-UserId") String preloginId, @Body EmptyBody emptyBody);
}
