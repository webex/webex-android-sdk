package com.cisco.spark.android.presence;

public class PostMessagePresenceEvent {
    private PresenceStatusList response;

    public PostMessagePresenceEvent(PresenceStatusList response) {
        this.response = response;
    }

    public PresenceStatusList getResponse() {
        return response;
    }
}
