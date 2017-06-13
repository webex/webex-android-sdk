package com.cisco.spark.android.client;

import retrofit.*;
import retrofit.client.*;

public class VoidCallback implements Callback<Void> {
    @Override
    public void success(Void aVoid, Response response) {
    }

    @Override
    public void failure(RetrofitError error) {
    }
}
