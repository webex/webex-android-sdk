package com.cisco.spark.android.metrics.model;

public enum CallAlertSourceType {
    CALL_ALERT_SOURCE_PUSH_FOREGROUND("PUSH_FOREGROUND"),
    CALL_ALERT_SOURCE_PUSH_BACKGROUND("PUSH_BACKGROUND"),
    CALL_ALERT_SOURCE_MERCURY("MERCURY"),
    CALL_ALERT_SOURCE_TOAST("TOAST");


    private final String source;

    private CallAlertSourceType(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return source;
    }
}
