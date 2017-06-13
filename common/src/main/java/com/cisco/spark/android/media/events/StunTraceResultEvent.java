package com.cisco.spark.android.media.events;


public class StunTraceResultEvent {
    private final String detail;

    public StunTraceResultEvent(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
