package com.cisco.spark.android.presence;

import java.util.ArrayList;
import java.util.List;

public class PresenceSubscriptionResponse {
    private ArrayList<Response> responses;


    public List<Response> getResponses() {
        return responses;
    }

    public static class Response {
        public String subject;
        public int responseCode;
        public int subscriptionTtl;
        public String evicted;
        public String message;
        public int retry;

        public String getSubject() {
            return subject;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public int getSubscriptionTtl() {
            return subscriptionTtl;
        }

        public String getEvicted() {
            return evicted;
        }

        public String getMessage() {
            return message;
        }

        public int getRetry() {
            return retry;
        }
    }
}
