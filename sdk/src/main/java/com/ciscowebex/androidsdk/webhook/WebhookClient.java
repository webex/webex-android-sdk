/*
 * Copyright 2016-2021 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.webhook;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * A client wrapper of the Cisco Webex <a href="https://developer.webex.com/docs/api/v1/webhooks">Webhooks REST API</a>
 *
 * @since 0.1
 */
public interface WebhookClient {

    /**
     * Lists all webhooks of the authenticated user.
     *
     * @param max     The maximum number of webhooks in the response.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(int max, @NonNull CompletionHandler<List<Webhook>> handler);

    /**
     * Posts a webhook for the authenticated user.
     *
     * @param name      A user-friendly name for this webhook.
     * @param targetUrl The URL that receives POST requests for each event.
     * @param resource  The resource type for the webhook.
     * @param event     The event type for the webhook.
     * @param filter    The filter that defines the webhook scope.
     * @param secret    Secret use to generate payload signiture
     * @param handler   A closure to be executed once the request has finished.
     * @since 0.1
     */
    void create(@NonNull String name, @NonNull String targetUrl, @NonNull String resource, @NonNull String event, @Nullable String filter, @Nullable String secret, @NonNull CompletionHandler<Webhook> handler);

    /**
     * Retrieves the details for a webhook by id.
     *
     * @param webhookId The identifier of  the webhook.
     * @param handler   A closure to be executed once the request has finished.
     * @since 0.1
     */
    void get(@NonNull String webhookId, @NonNull CompletionHandler<Webhook> handler);

    /**
     * Updates a webhook by id.
     *
     * @param webhookId The identifier of  the webhook.
     * @param name      A user-friendly name for this webhook.
     * @param targetUrl The URL that receives POST requests for each event.
     * @param handler   A closure to be executed once the request has finished.
     * @since 0.1
     */
    void update(@NonNull String webhookId, @NonNull String name, @NonNull String targetUrl, @NonNull CompletionHandler<Webhook> handler);

    /**
     * Updates a webhook by id.
     *
     * @param webhookId The identifier of  the webhook.
     * @param name      A user-friendly name for this webhook.
     * @param targetUrl The URL that receives POST requests for each event.
     * @param secret    The Secret used to generate payload signature.
     * @param status    The status of the webhook. Use "active" to reactivate a disabled webhook.
     * @param handler   A closure to be executed once the request has finished.
     * @since 1.4
     */
    void update(@NonNull String webhookId, @NonNull String name, @NonNull String targetUrl, @Nullable String secret, @Nullable String status, @NonNull CompletionHandler<Webhook> handler);


    /**
     * Deletes a webhook by id.
     *
     * @param webhookId The identifier of  the webhook.
     * @param handler   A closure to be executed once the request has finished.
     * @since 0.1
     */
    void delete(@NonNull String webhookId, @NonNull CompletionHandler<Void> handler);

}
