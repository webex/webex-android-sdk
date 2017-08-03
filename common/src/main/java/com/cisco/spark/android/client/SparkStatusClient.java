package com.cisco.spark.android.client;

import com.cisco.spark.android.status.GetSparkComponentsStatus;

import retrofit2.http.GET;
import rx.Observable;

public interface SparkStatusClient {

    @GET("index.json")
    Observable<GetSparkComponentsStatus> getComponentsStatus();

}
