package com.cisco.spark.android.whiteboard;

import org.joda.time.DateTime;
import java.util.UUID;

public class SnapshotRequest {
    private UUID requestId;
    private SnapshotRequestType requestType;
    private DateTime startTime;
    public static final int TIMEOUT_DURATION = 30; //Request timeout after 30 seconds

    private SnapshotRequest(SnapshotRequestType requestType) {
        requestId = UUID.randomUUID();
        this.requestType = requestType;
        startTime = DateTime.now();
    }

    public static SnapshotRequest getSnapshotRequest(SnapshotRequestType requestType) {
        return new SnapshotRequest(requestType);
    }

    public static SnapshotRequest getUploadSnapshotRequest() {
        return getSnapshotRequest(SnapshotRequestType.GET_SNAPSHOT_FOR_UPLOAD);
    }

    public static SnapshotRequest getPostSnapshotRequest() {
        return getSnapshotRequest(SnapshotRequestType.GET_SNAPSHOT_FOR_CHAT);
    }

    public SnapshotRequestType getRequestType() {
        return requestType;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public boolean isTimeOutRequest() {
        return DateTime.now().isAfter(startTime.plus(TIMEOUT_DURATION * 1000));
    }

    public enum SnapshotRequestType {
        GET_SNAPSHOT_FOR_UPLOAD,
        GET_SNAPSHOT_FOR_CHAT
    }
}

