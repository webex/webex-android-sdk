package com.cisco.spark.android.media.events;


public class MediaSessionEvent {
    private final String callId;

    public MediaSessionEvent(String callId) {
        this.callId = callId;
    }


    public String getCallId() {
        return callId;
    }
}
