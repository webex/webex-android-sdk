package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

public class WhiteboardChannelSavedEvent {
    private final boolean success;
    private final String errorMessage;
    private final Channel channel;

    public WhiteboardChannelSavedEvent(Channel channel, boolean success) {
        this.channel = channel;
        this.success = success;
        this.errorMessage = "";
    }

    public WhiteboardChannelSavedEvent(Channel channel, boolean success, String errorMessage) {
        this.channel = channel;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public Channel getChannel() {
        return channel;
    }
}
