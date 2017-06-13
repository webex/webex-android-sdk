package com.cisco.spark.android.whiteboard.view.event;

import com.cisco.spark.android.whiteboard.persistence.model.Content;

import java.util.List;

public class WhiteboardLoadContentsEvent {

    private List<Content> contents;
    private String boardId;
    private boolean shouldResetBoard;

    public WhiteboardLoadContentsEvent(List<Content> contents, String boardId, boolean shouldResetBoard) {
        this.contents = contents;
        this.boardId = boardId;
        this.shouldResetBoard = shouldResetBoard;
    }

    public String getBoardId() {
        return boardId;
    }
    public List<Content> getContents() {
        return contents;
    }
    public boolean shouldResetBoard() {
        return shouldResetBoard;
    }
}
