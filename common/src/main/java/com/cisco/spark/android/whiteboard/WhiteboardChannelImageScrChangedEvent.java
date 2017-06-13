package com.cisco.spark.android.whiteboard;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;

public class WhiteboardChannelImageScrChangedEvent {
    private Channel channel;
    private SecureContentReference secureContentReference;

    public WhiteboardChannelImageScrChangedEvent(Channel channel, SecureContentReference secureContentReference) {
        this.channel = channel;
        this.secureContentReference = secureContentReference;
    }

    public Channel getChannel() {
        return channel;
    }

    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }
}
