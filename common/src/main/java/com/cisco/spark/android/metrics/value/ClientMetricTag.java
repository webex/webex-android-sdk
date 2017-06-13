package com.cisco.spark.android.metrics.value;

public enum ClientMetricTag {
    METRIC_TAG_ACTIVITY_TYPE_TAG("activityType"),
    METRIC_TAG_SUCCESS_TAG("success"),
    METRIC_TAG_SELF_PRESENCE("selfPresence"),
    METRIC_TAG_REMOTE_PRESENCE("remotePresence");

    private String tagName;

    ClientMetricTag(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return this.tagName;
    }

    public String toString() {
        return this.tagName;
    }
}
