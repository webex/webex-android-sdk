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

package com.ciscospark.androidsdk.webhook;

import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.utils.http.ListBody;
import com.ciscospark.androidsdk.utils.http.ListCallback;
import com.ciscospark.androidsdk.utils.http.ObjectCallback;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
import me.helloworld.utils.collection.Maps;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class WebhookClient {

    private Authenticator _authenticator;

    private WebhookService _service;

    public WebhookClient(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(WebhookService.class);
    }

    public void list(int max, @NonNull CompletionHandler<List<Webhook>> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.list(s, max <= 0 ? null : max).enqueue(new ListCallback<>(handler));
        });
    }

    public void create(@NonNull String name, @NonNull String targetUrl, @NonNull String resource, @NonNull String event, @Nullable String filter, @Nullable String secret, @NonNull CompletionHandler<Webhook> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.create(s, Maps.makeMap("name", name, "targetUrl", targetUrl, "filter", filter, "secret", secret, "resource", resource, "event", event)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void get(@NonNull String webhookId, @NonNull CompletionHandler<Webhook> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.get(s, webhookId).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void update(@NonNull String webhookId, @NonNull String name, @NonNull String targetUrl, @NonNull CompletionHandler<Webhook> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.update(s, webhookId, Maps.makeMap("name", name, "targetUrl", targetUrl)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void delete(@NonNull String webhookId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.delete(s, webhookId).enqueue(new ObjectCallback<>(handler));
        });
    }

    private interface WebhookService {
        @GET("webhooks")
        Call<ListBody<Webhook>> list(@Header("Authorization") String authorization, @Query("max") Integer max);

        @POST("webhooks")
        Call<Webhook> create(@Header("Authorization") String authorization, @Body Map parameters);

        @GET("webhooks/{webhookId}")
        Call<Webhook> get(@Header("Authorization") String authorization, @Path("webhookId") String webhookId);

        @PUT("webhooks/{webhookId}")
        Call<Webhook> update(@Header("Authorization") String authorization, @Path("webhookId") String webhookId, @Body Map parameters);

        @DELETE("webhooks/{webhookId}")
        Call<Void> delete(@Header("Authorization") String authorization, @Path("webhookId") String webhookId);
    }
}
