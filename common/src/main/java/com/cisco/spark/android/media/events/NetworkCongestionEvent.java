package com.cisco.spark.android.media.events;


public class NetworkCongestionEvent extends MediaSessionEvent {
    public NetworkCongestionEvent(String callId) {
        super(callId);
    }
}
