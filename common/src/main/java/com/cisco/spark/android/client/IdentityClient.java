package com.cisco.spark.android.client;

import com.cisco.spark.android.model.UpdateUserRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface IdentityClient {

    @PATCH("Users/{userId}")
    Call<ResponseBody> updateUser(@Path(value = "userId") String userId, @Body UpdateUserRequest request);
}
