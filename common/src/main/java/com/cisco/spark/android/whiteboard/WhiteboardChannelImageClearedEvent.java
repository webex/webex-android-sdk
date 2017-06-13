package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

public class WhiteboardChannelImageClearedEvent {
    private Channel channel;

    public WhiteboardChannelImageClearedEvent(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }
}
