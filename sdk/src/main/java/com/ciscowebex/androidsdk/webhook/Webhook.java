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
import com.google.gson.annotations.SerializedName;

/**
 * A data type presents a Webhook at Cisco Webex for Developer.
 *
 * @see <a href="https://developer.webex.com/webhooks-explained.html">Webhook Explained</a>
 * @since 0.1
 */
public class Webhook {

    @SerializedName("id")
    private String _id;

    @SerializedName("name")
    private String _name;

    @SerializedName("targetUrl")
    private String _targetUrl;

    @SerializedName("resource")
    private String _resource;

    @SerializedName("event")
    private String _event;

    @SerializedName("filter")
    private String _filter;

    @SerializedName("secret")
    private String _secret;

    @SerializedName("created")
    private Date _created;

    @SerializedName("status")
    private String _status;

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
        return _id;
    }

    /**
     * @return A user-friendly name for this webhook.
     * @since 0.1
     */
    public String getName() {
        return _name;
    }

    /**
     * @return The URL that receives POST requests for each event.
     * @since 0.1
     */
    public String getTargetUrl() {
        return _targetUrl;
    }

    /**
     * @return The resource type for the webhook.
     * @since 0.1
     */
    public String getResource() {
        return _resource;
    }

    /**
     * @return The event type for the webhook.
     * @since 0.1
     */
    public String getEvent() {
        return _event;
    }

    /**
     * @return The filter that defines the webhook scope.
     * @since 0.1
     */
    public String getFilter() {
        return _filter;
    }

    /**
     * @return The secret for the webhook.
     * @since 0.1
     */
    public String getSecret() {
        return _secret;
    }

    /**
     * @return The timestamp that the webhook being created.
     * @since 0.1
     */
    public Date getCreated() {
        return _created;
    }
}
