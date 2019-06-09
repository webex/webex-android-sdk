/*
 * Copyright 2016-2019 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.webhook.internal;

import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ListCallback;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.ciscowebex.androidsdk.webhook.Webhook;
import com.ciscowebex.androidsdk.webhook.WebhookClient;

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

public class WebhookClientImpl implements WebhookClient {

    private Authenticator _authenticator;

    private WebhookService _service;

    private static final String KEY_TARGET_URL = "targetUrl";

    public WebhookClientImpl(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(WebhookService.class);
    }

    public void list(int max, @NonNull CompletionHandler<List<Webhook>> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.list(s, max <= 0 ? null : max), new ListCallback<>(handler));
    }

    public void create(@NonNull String name, @NonNull String targetUrl, @NonNull String resource, @NonNull String event, @Nullable String filter, @Nullable String secret, @NonNull CompletionHandler<Webhook> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.create(s, Maps.makeMap("name", name, KEY_TARGET_URL, targetUrl, "filter", filter, "secret", secret, "resource", resource, "event", event)), new ObjectCallback<>(handler));
    }

    public void get(@NonNull String webhookId, @NonNull CompletionHandler<Webhook> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.get(s, webhookId), new ObjectCallback<>(handler));
    }

    public void update(@NonNull String webhookId, @NonNull String name, @NonNull String targetUrl, @NonNull CompletionHandler<Webhook> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.update(s, webhookId, Maps.makeMap("name", name, KEY_TARGET_URL, targetUrl)), new ObjectCallback<>(handler));
    }

    @Override
    public void update(@NonNull String webhookId, @NonNull String name, @NonNull String targetUrl, @Nullable String secret, @Nullable String status, @NonNull CompletionHandler<Webhook> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.update(s, webhookId, Maps.makeMap("name", name, KEY_TARGET_URL, targetUrl, "secret", secret, "status", status)), new ObjectCallback<>(handler));
    }

    public void delete(@NonNull String webhookId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.delete(s, webhookId), new ObjectCallback<>(handler));
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
