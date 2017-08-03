package com.cisco.spark.android.media.events;


public class NetworkLostEvent extends MediaSessionEvent {
    public NetworkLostEvent(String callId) {
        super(callId);
    }
}
