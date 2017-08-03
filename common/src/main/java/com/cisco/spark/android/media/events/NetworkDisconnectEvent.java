package com.cisco.spark.android.media.events;


public class NetworkDisconnectEvent extends MediaSessionEvent {
    private String mediaType;

    public NetworkDisconnectEvent(String callId, String mediaType) {
        super(callId);
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }
}
