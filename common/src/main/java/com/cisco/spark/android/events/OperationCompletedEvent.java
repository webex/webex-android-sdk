package com.cisco.spark.android.events;

import android.support.annotation.NonNull;

import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;
import com.cisco.spark.android.sync.operationqueue.core.Operation;

public class OperationCompletedEvent {

    private Operation operation;

    public OperationCompletedEvent(@NonNull Operation operation) {
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isType(SyncOperationEntry.OperationType type) {
        return operation.getOperationType() == type;
    }

    public boolean isFaulted() {
        return operation.getState() == SyncOperationEntry.SyncState.FAULTED;
    }

    public boolean isCanceled() {
        return isFaulted()
                && operation.getFailureReason() == SyncOperationEntry.SyncStateFailureReason.CANCELED;
    }

    public boolean isSucceeded() {
        return operation.getState() == SyncOperationEntry.SyncState.SUCCEEDED;
    }
}
