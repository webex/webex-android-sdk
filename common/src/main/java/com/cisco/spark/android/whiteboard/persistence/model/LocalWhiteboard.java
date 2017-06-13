package com.cisco.spark.android.whiteboard.persistence.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LocalWhiteboard {

    private final String localChannelId;
    private Channel channel;
    private final List<Content> content;

    public LocalWhiteboard(String localChannelId, Channel channel) {
        this.localChannelId = localChannelId;
        this.channel = channel;
        content = new ArrayList<>();
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public List<Content> getContent() {
        return content;
    }

    public void addContent(List<Content> content) {
        this.content.addAll(content);
    }

    public void addContent(Content content) {
        this.content.add(content);
    }
}
