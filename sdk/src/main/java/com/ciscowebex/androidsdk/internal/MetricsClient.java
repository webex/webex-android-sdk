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

package com.ciscowebex.androidsdk.internal;

import java.util.List;
import java.util.Map;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.github.benoitdion.ln.Ln;

import me.helloworld.utils.collection.Maps;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class MetricsClient {

    private Authenticator _authenticator;

    private MetricsService _service;

    public MetricsClient(Authenticator authenticator, String URL) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().baseURL(URL).build(MetricsService.class);
    }

    public void post(List<Map<String, String>> metrics) {
        _authenticator.getToken(result -> {
            String token = result.getData();
            if (token != null) {
                _service.post("Bearer " + token, Maps.makeMap("metrics", metrics)).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Ln.d("%s", response);
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Ln.e(t);
                    }
                });
            }
        });
    }

    interface MetricsService {

        @POST("metrics")
        Call<Void> post(@Header("Authorization") String authorization, @Body Map parameters);
    }

}
