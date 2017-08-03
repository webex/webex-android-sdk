package com.cisco.spark.android.whiteboard.view.event;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Content;

import java.util.List;

public class WhiteboardLoadContentsEvent {

    private List<Content> contents;
    private Channel channel;
    private boolean shouldResetBoard;

    public WhiteboardLoadContentsEvent(List<Content> contents, Channel boardId, boolean shouldResetBoard) {
        this.contents = contents;
        this.channel = boardId;
        this.shouldResetBoard = shouldResetBoard;
    }

    public String getBoardId() {
        return channel != null ? channel.getChannelId() : null;
    }
    public List<Content> getContents() {
        return contents;
    }
    public boolean shouldResetBoard() {
        return shouldResetBoard;
    }

    public Channel getChannel() {
        return channel;
    }
}
