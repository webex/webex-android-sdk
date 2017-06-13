package com.cisco.spark.android.room;

public class AltoMetricsTokenRefreshedEvent {

    private final long refreshTimeMillis;

    public AltoMetricsTokenRefreshedEvent(long refreshTimeMillis) {
        this.refreshTimeMillis = refreshTimeMillis;
    }

    public long getRefreshTimeMillis() {
        return refreshTimeMillis;
    }

}
