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

package com.ciscowebex.androidsdk.webhook;

import java.util.Date;

import com.google.gson.Gson;

/**
 * A data type presents a Webhook at Cisco Webex for Developer.
 *
 * @see <a href="https://developer.webex.com/docs/api/guides/webhooks">Webhooks Guide</a>
 * @since 0.1
 */
public class Webhook {

    private String id;
    private String name;
    private String targetUrl;
    private String resource;
    private String event;
    private String filter;
    private String secret;
    private Date created;
    private String status;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * @return The identifier of this webhook.
     * @since 0.1
     */
    public String getId() {
        return id;
    }

    /**
     * @return A user-friendly name for this webhook.
     * @since 0.1
     */
    public String getName() {
        return name;
    }

    /**
     * @return The URL that receives POST requests for each event.
     * @since 0.1
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * @return The resource type for the webhook.
     * @since 0.1
     */
    public String getResource() {
        return resource;
    }

    /**
     * @return The event type for the webhook.
     * @since 0.1
     */
    public String getEvent() {
        return event;
    }

    /**
     * @return The filter that defines the webhook scope.
     * @since 0.1
     */
    public String getFilter() {
        return filter;
    }

    /**
     * @return The secret for the webhook.
     * @since 0.1
     */
    public String getSecret() {
        return secret;
    }

    /**
     * @return The timestamp that the webhook being created.
     * @since 0.1
     */
    public Date getCreated() {
        return created;
    }
}
