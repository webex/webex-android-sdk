package com.cisco.spark.android.media.events;


public class NetworkReconnectEvent extends MediaSessionEvent {
    private String mediaType;

    public NetworkReconnectEvent(String callId) {
        this(callId, "Unknown");
    }

    public NetworkReconnectEvent(String callId, String mediaType) {
        super(callId);
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }
}
