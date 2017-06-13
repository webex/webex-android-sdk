package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

public class WhiteboardChannelInfoReadyEvent {
    private Channel channel;

    public WhiteboardChannelInfoReadyEvent(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }
}
