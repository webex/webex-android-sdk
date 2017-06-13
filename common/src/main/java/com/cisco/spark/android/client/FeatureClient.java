package com.cisco.spark.android.client;

import com.cisco.spark.android.wdm.FeatureToggle;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;


public interface FeatureClient {
    @POST("features/users/{userId}")
    Call<FeatureToggle> toggleFeature(@Path("userId") String userId, @Body FeatureToggle feature);

    @POST("features/users/{userId}/developer")
    Call<FeatureToggle> toggleDeveloperFeature(@Path("userId") String userId, @Body FeatureToggle feature);

    @GET("features/users/{userId}/developer/{key}")
    Call<FeatureToggle> getDeveloperFeature(@Path("userId") String userId, @Path("key") String key);

    @DELETE("features/users/{userId}/user/{key}")
    Call<Void> unsetUserFeature(@Path("userId") String userId, @Path("key") String key);

    @POST("features/users/{userId}/user")
    Call<FeatureToggle> toggleUserFeature(@Path("userId") String userId, @Body FeatureToggle feature);

    @POST("features/users/{userId}/toggles")
    Call<Void> toggleFeatures(@Path("userId") String userId, @Body List<FeatureToggle> features);
}
