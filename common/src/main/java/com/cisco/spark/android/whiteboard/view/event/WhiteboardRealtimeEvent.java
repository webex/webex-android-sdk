package com.cisco.spark.android.whiteboard.view.event;

import com.google.gson.JsonObject;

public class WhiteboardRealtimeEvent {
    private String boardId;
    private JsonObject data;

    public WhiteboardRealtimeEvent(JsonObject data, String boardId) {
        this.data = data;
        this.boardId = boardId;
    }

    public JsonObject getData() {
        return data;
    }

    public String getBoardId() {
        return boardId;
    }
}
