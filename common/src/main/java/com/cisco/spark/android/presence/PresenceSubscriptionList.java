package com.cisco.spark.android.presence;

import android.net.Uri;

import java.util.ArrayList;

public class PresenceSubscriptionList {
    private String subject;
    private boolean truncated;
    private ArrayList<Subscription> subscriptions;

    public static class Subscription {
        private Uri deviceUrl;
        private String subject;
        private int subscriptionTtl;

        public Uri getDeviceUrl() {
            return deviceUrl;
        }

        public String getSubject() {
            return subject;
        }

        public int getSubscriptionTtl() {
            return subscriptionTtl;
        }
    }
}
