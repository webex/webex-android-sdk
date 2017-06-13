package com.cisco.spark.android.whiteboard;

public class WhiteboardSendSnapshotToChatEvent {
    private java.io.File image;

    public WhiteboardSendSnapshotToChatEvent(java.io.File image) {
        this.image = image;
    }

    public java.io.File getImage() {
        return image;
    }
}
