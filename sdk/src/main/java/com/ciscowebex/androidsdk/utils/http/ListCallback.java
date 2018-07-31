/*
 * Copyright 2016-2017 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.utils.http;

import java.util.List;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.ResultImpl;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class ListCallback<T> extends ListenerCallback<ListBody<T>> {

    private CompletionHandler<List<T>> _handler;

    public ListCallback(CompletionHandler<List<T>> handler) {
        _handler = handler;
    }

    @Override
    public void onResponse(Call<ListBody<T>> call, Response<ListBody<T>> response) {
        if (response.isSuccessful()) {
            _handler.onComplete(ResultImpl.success(response.body().getItems()));
        } else if (!checkUnauthError(response)) {
            _handler.onComplete(ResultImpl.error(response));
        }
    }

    @Override
    public void onFailure(Call<ListBody<T>> call, Throwable t) {
        _handler.onComplete(ResultImpl.error(t));
    }

}
