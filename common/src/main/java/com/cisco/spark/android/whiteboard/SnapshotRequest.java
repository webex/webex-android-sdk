package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

import org.joda.time.DateTime;

import java.util.UUID;

public class SnapshotRequest {
    private String requestId;
    private SnapshotRequestType requestType;
    private DateTime startTime;
    private Channel channel;
    private String aclId;
    public static final int TIMEOUT_DURATION = 30; //Request timeout after 30 seconds

    private SnapshotRequest(SnapshotRequestType requestType, Channel channel, String aclId) {
        requestId = UUID.randomUUID().toString();
        this.requestType = requestType;
        this.channel = channel;
        this.aclId = aclId;
        startTime = DateTime.now();
    }

    public static SnapshotRequest getSnapshotRequest(SnapshotRequestType requestType, Channel channel, String aclId) {
        return new SnapshotRequest(requestType, channel, aclId);
    }

    public static SnapshotRequest getUploadSnapshotRequest(Channel channel, String aclId) {
        return getSnapshotRequest(SnapshotRequestType.GET_SNAPSHOT_FOR_UPLOAD, channel, aclId);
    }

    public static SnapshotRequest getPostSnapshotRequest() {
        return getSnapshotRequest(SnapshotRequestType.GET_SNAPSHOT_FOR_CHAT, null, null);
    }

    public Channel getChannel() {
        return channel;
    }

    public String getAclId() {
        return aclId;
    }

    public SnapshotRequestType getRequestType() {
        return requestType;
    }

    public String getRequestId() {
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

