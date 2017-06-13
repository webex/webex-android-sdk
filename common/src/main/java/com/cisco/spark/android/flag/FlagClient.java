package com.cisco.spark.android.flag;

import com.cisco.spark.android.model.ItemCollection;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FlagClient {
    @POST("flags")
    Call<Flag> flag(@Body Flag request);

    @DELETE("flags/{id}")
    Call<Void> delete(@Path("id") String id);

    @GET("flags?state=flagged")
    Call<ItemCollection<Flag>> getFlags();
}
