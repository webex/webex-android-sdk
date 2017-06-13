package com.cisco.spark.android.model;


public class ActivitySearchResponse {
    String responseId;
    ItemCollection<Activity> activities;
    String failureReason;
    String kmsMessage;

    public String getResponseId() {
        return responseId;
    }

    public ItemCollection<Activity> getActivities() {
        return activities;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getKmsMessage() {
        return kmsMessage;
    }

    public void setKmsMessage(String kmsMessage) {
        this.kmsMessage = kmsMessage;
    }

}
