package com.cisco.spark.android.whiteboard;

public class SnapshotUploadOperationCompleteEvent {
    private final boolean isSuccess;
    private final String operationId;

    public SnapshotUploadOperationCompleteEvent(final String operationId, final boolean isSuccess) {
        this.operationId = operationId;
        this.isSuccess = isSuccess;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getOperationId() {
        return operationId;
    }
}
