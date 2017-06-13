package com.cisco.spark.android.whiteboard;

import java.util.UUID;

public class WhiteboardSnapshotEvent {
    private String imageString;
    private java.io.File image;
    private UUID requestId;
    private SnapshotRequest.SnapshotRequestType requestType;

    public WhiteboardSnapshotEvent(String imageString, java.io.File image, UUID requestId, SnapshotRequest.SnapshotRequestType requestType) {
        this.imageString = imageString;
        this.image = image;
        this.requestId = requestId;
        this.requestType = requestType;
    }

    public static WhiteboardSnapshotEvent whiteboardSnapshotEvent(String imageString, java.io.File image, UUID requestId, SnapshotRequest.SnapshotRequestType requestType) {
        return new WhiteboardSnapshotEvent(imageString, image, requestId, requestType);
    }

    public String getImageString() {
        return  imageString;
    }

    public java.io.File getImage() {
        return image;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public SnapshotRequest.SnapshotRequestType getRequestType() {
        return requestType;
    }
}
