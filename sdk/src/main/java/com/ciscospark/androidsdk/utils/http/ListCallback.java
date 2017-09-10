package com.ciscospark.androidsdk.utils.http;

import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class ListCallback<T> implements Callback<ListBody<T>> {

    private CompletionHandler<List<T>> _handler;

    public ListCallback(CompletionHandler<List<T>> handler) {
        _handler = handler;
    }

    @Override
    public void onResponse(Call<ListBody<T>> call, Response<ListBody<T>> response) {
        if (response.isSuccessful()) {
            _handler.onComplete(Result.success(response.body().getItems()));
        } else {
            _handler.onComplete(Result.error(response));
        }
    }

    @Override
    public void onFailure(Call<ListBody<T>> call, Throwable t) {
        _handler.onComplete(Result.error(t));
    }

}
