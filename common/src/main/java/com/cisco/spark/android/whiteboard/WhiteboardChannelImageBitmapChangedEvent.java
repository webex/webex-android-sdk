package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

public class WhiteboardChannelImageBitmapChangedEvent {
    private Channel channel;
    private byte[] bitmapBytes;

    public WhiteboardChannelImageBitmapChangedEvent(Channel channel, byte[] bitmapBytes) {
        this.channel = channel;
        this.bitmapBytes = bitmapBytes;
    }

    public Channel getChannel() {
        return channel;
    }

    public byte[] getBitmapBytes() {
        return bitmapBytes;
    }
}
