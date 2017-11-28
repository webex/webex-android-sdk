package com.ciscospark.androidsdk.utils.http;

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

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.internal.ResultImpl;
import com.google.gson.Gson;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by zhiyuliu on 31/08/2017.
 */

public class ServiceBuilder {

    public static final String HYDRA_URL = "https://api.ciscospark.com/v1/";

    private String _baseURL = HYDRA_URL;

    private Gson _gson;

    private List<Interceptor> _interceptors = new ArrayList<>(1);

    private boolean _interceptorChanged = false;

    public ServiceBuilder() {
        _interceptors.add(new DefaultHeadersInterceptor());
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.i("RetrofitLog","retrofitBack = "+message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        _interceptors.add(loggingInterceptor);
    }

    public ServiceBuilder baseURL(String url) {
        _baseURL = url;
        return this;
    }

    public ServiceBuilder gson(Gson gson) {
        _gson = gson;
        return this;
    }

    public ServiceBuilder interceptor(Interceptor interceptor) {
        if (!_interceptorChanged) {
            _interceptors.clear();
            _interceptorChanged = true;
        }
        if (interceptor != null) {
            _interceptors.add(interceptor);
        }
        return this;
    }

    public <T> T build(Class<T> service) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        for (Interceptor interceptor : _interceptors) {
            httpClient.addInterceptor(interceptor);
        }
        OkHttpClient client = httpClient.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(_baseURL)
                .addConverterFactory(_gson == null ? GsonConverterFactory.create() : GsonConverterFactory.create(_gson))
                .client(client)
                .build();
        return retrofit.create(service);
    }

    public static <T> void async(Authenticator authenticator, CompletionHandler<T> handler, Closure<String> closure) {
        authenticator.getToken(new CompletionHandler<String>() {
            @Override
            public void onComplete(Result<String> result) {
                String token = result.getData();
                if (token != null) {
                    closure.invoke("Bearer " + token);
                }
                else {
                    if (handler != null) {
                        handler.onComplete(ResultImpl.error(result.getError()));
                    }
                }
            }
        });
    }

    public interface Closure<P> {
        void invoke(P p);
    }
}
