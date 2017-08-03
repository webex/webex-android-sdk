package com.cisco.spark.android.lyra;

import retrofit2.Response;

public interface BindingCallback {

    void onSuccess(Response response);

    void onError(Response response);
}
