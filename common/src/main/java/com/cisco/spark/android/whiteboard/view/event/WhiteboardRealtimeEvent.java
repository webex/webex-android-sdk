package com.cisco.spark.android.whiteboard.view.event;

public class WhiteboardRealtimeEvent {

    private String data;

    public WhiteboardRealtimeEvent(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
