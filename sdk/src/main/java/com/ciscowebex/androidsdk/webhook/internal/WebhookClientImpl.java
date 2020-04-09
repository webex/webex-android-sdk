/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.model.ItemsModel;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.webhook.Webhook;
import com.ciscowebex.androidsdk.webhook.WebhookClient;
import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.collection.Maps;

public class WebhookClientImpl implements WebhookClient {

    private Authenticator authenticator;

    private static final String KEY_TARGET_URL = "targetUrl";

    public WebhookClientImpl(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void list(int max, @NonNull CompletionHandler<List<Webhook>> handler) {
        Service.Hydra.get("webhooks")
                .with("max", max <= 0 ? null : String.valueOf(max))
                .auth(authenticator)
                .queue(Queue.main)
                .model(new TypeToken<ItemsModel<Webhook>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<Webhook>>) result -> handler.onComplete(ResultImpl.success(result.getItems())));
    }

    public void get(@NonNull String webhookId, @NonNull CompletionHandler<Webhook> handler) {
        Service.Hydra.get("webhooks", webhookId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(Webhook.class)
                .error(handler)
                .async((Closure<Webhook>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void create(@NonNull String name, @NonNull String targetUrl, @NonNull String resource, @NonNull String event, @Nullable String filter, @Nullable String secret, @NonNull CompletionHandler<Webhook> handler) {
        Service.Hydra.post(Maps.makeMap("name", name, KEY_TARGET_URL, targetUrl, "filter", filter, "secret", secret, "resource", resource, "event", event))
                .to("webhooks")
                .auth(authenticator)
                .queue(Queue.main)
                .model(Webhook.class)
                .error(handler)
                .async((Closure<Webhook>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void update(@NonNull String webhookId, @NonNull String name, @NonNull String targetUrl, @NonNull CompletionHandler<Webhook> handler) {
        Service.Hydra.put(Maps.makeMap("name", name, KEY_TARGET_URL, targetUrl))
                .to("webhooks", webhookId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(Webhook.class)
                .error(handler)
                .async((Closure<Webhook>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void update(@NonNull String webhookId, @NonNull String name, @NonNull String targetUrl, @Nullable String secret, @Nullable String status, @NonNull CompletionHandler<Webhook> handler) {
        Service.Hydra.put(Maps.makeMap("name", name, KEY_TARGET_URL, targetUrl, "secret", secret, "status", status))
                .to("webhooks", webhookId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(Webhook.class)
                .error(handler)
                .async((Closure<Webhook>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void delete(@NonNull String webhookId, @NonNull CompletionHandler<Void> handler) {
        Service.Hydra.delete("webhooks", webhookId)
                .auth(authenticator)
                .queue(Queue.main)
                .error(handler)
                .async((Closure<Void>) result -> handler.onComplete(ResultImpl.success(result)));
    }

}
