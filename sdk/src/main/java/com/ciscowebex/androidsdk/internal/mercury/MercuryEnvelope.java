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

package com.ciscowebex.androidsdk.internal.mercury;

import android.text.TextUtils;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MercuryEnvelope {

    public static class Recipient {

        private String alertType;
        private String route;

        public String getRoute() {
            return route;
        }
    }

    private static final String ROUTE_PREFIX = "board.";

    private String id;
    private String type;
    private MercuryEvent data;
    private Boolean isDeliveryEscalation;
    private Headers headers;
    private boolean filterMessage;
    private List<Recipient> recipients;

    public MercuryEnvelope() {
        recipients = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public MercuryEvent getData() {
        return data;
    }

    public boolean isDeliveryEscalation() {
        return isDeliveryEscalation == null ? false : isDeliveryEscalation;
    }

    public Headers getHeaders() {
        return headers;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public static class Headers {

        @SerializedName("data.activity.target.lastReadableActivityDate")
        private Date lastReadableActivityDate;

        @SerializedName("data.activity.target.getLastSeenActivityDate")
        private Date lastSeenActivityDate;

        @SerializedName("data.activity.target.lastRelevantActivityDate")
        private Date lastRelevantActivityDate;

        private String route;

        public String getChannelId() {
            return getChannelIdFromRoute(route);
        }

        private String getChannelIdFromRoute(String route) {
            if (!TextUtils.isEmpty(this.route) && this.route.startsWith(ROUTE_PREFIX)) {
                route = this.route.substring(ROUTE_PREFIX.length());
                if (!TextUtils.isEmpty(route)) {
                    route = route.replace(".", "-");
                }
            }
            return route;
        }

        public Date getLastReadableActivityDate() {
            return lastReadableActivityDate;
        }

        public Date getLastSeenActivityDate() {
            return lastSeenActivityDate;
        }
        public Date getLastRelevantActivityDate() {
            return lastRelevantActivityDate;
        }
    }
}
