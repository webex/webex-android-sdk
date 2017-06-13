package com.cisco.spark.android.whiteboard;

public class SelfRemovedFromAclEvent {
    private boolean success;
    private String channelId;

    public SelfRemovedFromAclEvent(boolean success, String channelId) {
        this.success = success;
        this.channelId = channelId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getChannelId() {
        return channelId;
    }
}
