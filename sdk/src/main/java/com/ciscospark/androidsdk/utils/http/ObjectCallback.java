package com.ciscospark.androidsdk.utils.http;

import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class ObjectCallback<T> implements Callback<T> {

    private CompletionHandler<T> _handler;

    public ObjectCallback(CompletionHandler<T> handler) {
        _handler = handler;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful()) {
            _handler.onComplete(Result.success(response.body()));
        } else {
            _handler.onComplete(Result.error(response));
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        _handler.onComplete(Result.error(t));
    }
}
