package com.cisco.spark.android.client;

import com.cisco.spark.android.metrics.model.GenericMetricsRequest;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.status.HealthCheckResponse;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import rx.Observable;

public interface MetricsClient {
    @POST("metrics")
    Call<Void> postGenericMetric(@Body MetricsReportRequest request);

    @POST("clientmetrics")
    Call<Void> postClientMetric(@Body GenericMetricsRequest request);

    @GET("ping")
    Observable<Response<HealthCheckResponse>> ping();
}
