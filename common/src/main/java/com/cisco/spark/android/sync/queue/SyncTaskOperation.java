package com.cisco.spark.android.sync.queue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;

import java.io.IOException;


public class SyncTaskOperation extends Operation {

    transient private final AbstractConversationSyncTask syncTask;

    public SyncTaskOperation(Injector injector, AbstractConversationSyncTask syncTask) {
        super(injector);
        this.syncTask = syncTask;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.GENERAL_SYNC_TASK;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        syncTask.execute();
        return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;
    }

    @Override
    public boolean shouldPersist() {
        // Making the abstract sync task serializable is a bit of work. No real need to persist
        // these anyway as long as the HWM is reliable.
        return false;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }
}
