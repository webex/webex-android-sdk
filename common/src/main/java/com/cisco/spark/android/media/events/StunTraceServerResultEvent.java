package com.cisco.spark.android.media.events;

public class StunTraceServerResultEvent {

    private final String detail;

    public StunTraceServerResultEvent(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
