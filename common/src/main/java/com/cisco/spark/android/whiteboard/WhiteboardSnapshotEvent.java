package com.cisco.spark.android.whiteboard;

public class WhiteboardSnapshotEvent {
    private String imageString;
    private java.io.File image;
    private String requestId;
    private SnapshotRequest.SnapshotRequestType requestType;

    public WhiteboardSnapshotEvent(String imageString, java.io.File image, String requestId, SnapshotRequest.SnapshotRequestType requestType) {
        this.imageString = imageString;
        this.image = image;
        this.requestId = requestId;
        this.requestType = requestType;
    }

    public static WhiteboardSnapshotEvent whiteboardSnapshotEvent(String imageString, java.io.File image, String requestId, SnapshotRequest.SnapshotRequestType requestType) {
        return new WhiteboardSnapshotEvent(imageString, image, requestId, requestType);
    }

    public String getImageString() {
        return  imageString;
    }

    public java.io.File getImage() {
        return image;
    }

    public String getRequestId() {
        return requestId;
    }

    public SnapshotRequest.SnapshotRequestType getRequestType() {
        return requestType;
    }
}
