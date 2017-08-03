package com.cisco.spark.android.client;

import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.MultiRetentionPolicyRequest;
import com.cisco.spark.android.model.RetentionPolicy;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetentionClient {

    @POST("organizations")
    Call<ItemCollection<RetentionPolicy>> getMultiRetentionPolicy(@Body MultiRetentionPolicyRequest multiRetentionPolicyRequest);
}
