package com.cisco.spark.android.authenticator;

import com.cisco.spark.android.authenticator.model.InvokeUserActivationRequest;
import com.cisco.spark.android.authenticator.model.InvokeUserActivationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface IdbrokerTokenClient {

    @POST("actions/UserActivation/invoke")
    Call<InvokeUserActivationResponse> postInvokeUserActivation(@Body InvokeUserActivationRequest request);
}
