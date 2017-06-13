package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.FetchActivityContextTask;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.FETCH_ACTIVITY_CONTEXT;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class FetchActivityContextOperation extends Operation implements ConversationOperation {
    private static final int MAX_ACTIVITIES = 50;

    private String conversationId;
    private long activityTimestamp;

    public FetchActivityContextOperation(Injector injector, String conversationId, long activityTimestamp) {
        super(injector);
        this.conversationId = conversationId;
        this.activityTimestamp = activityTimestamp;
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return FETCH_ACTIVITY_CONTEXT;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        if (activityTimestamp <= 0) {
            Ln.e("Error fetching activity context, invalid timestamp %s", String.valueOf(activityTimestamp));
            return ConversationContract.SyncOperationEntry.SyncState.FAULTED;
        }

        Ln.d("FetchActivityContextOperation enqueued for conversation %s", conversationId);
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        FetchActivityContextTask task = new FetchActivityContextTask(injector);

        task.setMaxActivities(MAX_ACTIVITIES);
        task.setTimestamp(activityTimestamp);
        task.setConversationId(conversationId);
        task.execute();

        if (task.succeeded()) {
            return SyncState.SUCCEEDED;
        } else {
            return SyncState.READY;
        }
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        //isOperationRedundant handles dupes, so we don't have to worry about this
    }

    @Override
    protected ConversationContract.SyncOperationEntry.SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(5, TimeUnit.MINUTES);
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == FETCH_ACTIVITY_CONTEXT && conversationId.equals(((FetchActivityContextOperation) newOperation).getConversationId());
    }
}
