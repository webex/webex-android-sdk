package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

import java.util.UUID;

public class WhiteboardCreationCompleteEvent {

    private final UUID channelCreationRequestId;

    private final Channel channel;

    public WhiteboardCreationCompleteEvent(UUID channelCreationRequestId, Channel channel) {
        this.channelCreationRequestId = channelCreationRequestId;
        this.channel = channel;
    }

    public UUID getChannelCreationRequestId() {
        return channelCreationRequestId;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getBoardId() {
        return channel.getChannelId();
    }
}
