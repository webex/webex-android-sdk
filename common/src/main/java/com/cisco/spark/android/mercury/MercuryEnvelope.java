package com.cisco.spark.android.mercury;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MercuryEnvelope {

    private static final String ROUTE_PREFIX = "board.";

    private String id;
    private MercuryData data;
    private AlertType alertType;
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

    public MercuryData getData() {
        return data;
    }

    public AlertType getAlertType() {
        return alertType == null ? AlertType.NONE : alertType;
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

    public static class Recipient {

        private String alertType;
        private String route;

        public String getRoute() {
            return route;
        }
    }
}
