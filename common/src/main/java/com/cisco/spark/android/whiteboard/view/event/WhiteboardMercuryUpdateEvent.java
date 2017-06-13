package com.cisco.spark.android.whiteboard.view.event;

import com.cisco.spark.android.whiteboard.persistence.model.Content;

import java.util.List;

public class WhiteboardMercuryUpdateEvent {
    private boolean isClearAll;
    private String boardId;
    private List<Content> contents;

    public WhiteboardMercuryUpdateEvent(List<Content> contents, String boardId, boolean isClearAll) {
        this.contents = contents;
        this.boardId = boardId;
        this.isClearAll = isClearAll;
    }

    public static WhiteboardMercuryUpdateEvent createClearAllEvent() {
        return new WhiteboardMercuryUpdateEvent(null, null, true);
    }

    public boolean isClearAll() {
        return isClearAll;
    }

    public String getBoardId() {
        return boardId;
    }

    public List<Content> getContents() {
        return contents;
    }
}
