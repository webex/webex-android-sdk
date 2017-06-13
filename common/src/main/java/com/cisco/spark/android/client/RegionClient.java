package com.cisco.spark.android.client;

import com.cisco.spark.android.model.RegionInfo;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RegionClient {
    @GET("v1/region")
    Call<RegionInfo> getRegion();
}
