package com.cisco.spark.android.client;

import retrofit2.Call;

public class VoidCallback implements retrofit2.Callback<Void> {
    public static final VoidCallback instance = new VoidCallback();

    @Override
    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
    }

    @Override
    public void onFailure(Call<Void> call, Throwable t) {
    }
}
