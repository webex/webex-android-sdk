package com.cisco.spark.android.sync.operationqueue;


import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationRecord;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.google.gson.Gson;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;


import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for incrementing share count
 */
public class IncrementShareCountOperation extends Operation {

    @Inject
    transient protected Gson gson;

    @Inject
    transient Provider<Batch> batchProvider;

    protected String conversationId;

    public IncrementShareCountOperation(Injector injector, String conversationId) {
        super(injector);
        this.conversationId = conversationId;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.INCREMENT_SHARE_COUNT;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        ConversationRecord conversationRecord = ConversationRecord.buildFromContentResolver(getContentResolver(), gson, conversationId, null);
        Batch batch = batchProvider.get();
        conversationRecord.addShareCountOperation(batch, conversationRecord.getShareCount() + 1);
        batch.apply();

        return SyncState.SUCCEEDED;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(3);
    }
}
