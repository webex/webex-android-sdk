package com.cisco.spark.android.whiteboard;

import android.support.annotation.NonNull;

public class SelfRemovedFromAclEvent {

    private boolean success;
    private String channelId;

    public SelfRemovedFromAclEvent(boolean success, @NonNull String channelId) {
        this.success = success;
        this.channelId = channelId;
    }

    public boolean isSuccess() {
        return success;
    }

    @NonNull
    public String getChannelId() {
        return channelId;
    }
}
