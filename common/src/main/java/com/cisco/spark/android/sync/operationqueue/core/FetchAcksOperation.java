package com.cisco.spark.android.sync.operationqueue.core;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.ConversationOperation;
import com.cisco.spark.android.sync.queue.ConversationFrontFillTask;
import com.cisco.spark.android.sync.queue.IncrementalSyncTask;

import java.io.IOException;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;

public class FetchAcksOperation extends Operation implements ConversationOperation {
    private final String conversationId;

    public FetchAcksOperation(Injector injector, String conversationId) {
        super(injector);
        this.conversationId = conversationId;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.FETCH_ACKS;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        IncrementalSyncTask task = new ConversationFrontFillTask(injector, conversationId)
                .withMaxParticipants(0);
        task.execute();
        return SUCCEEDED;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != getOperationType())
            return false;

        FetchAcksOperation newFetchParticipantsOp = (FetchAcksOperation) newOperation;

        return TextUtils.equals(newFetchParticipantsOp.getConversationId(), getConversationId());
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }
}
