package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.sync.queue.MentionsTask;
import com.cisco.spark.android.util.DateUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.FETCH_MENTIONS;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class FetchMentionsOperation extends Operation {
    private static final int MAX_ACTIVITIES = 50;
    private long sinceDate;

    @Inject
    transient Settings settings;

    @Inject
    transient ConversationSyncQueue conversationSyncQueue;

    public FetchMentionsOperation(Injector injector) {
        super(injector);
        sinceDate = DateUtils.getTimestampDaysAgo(28);
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return FETCH_MENTIONS;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        // If we already did this, no need to do it again
        if (settings.hasLoadedMentions()) {
            cancel();
            return SyncState.FAULTED;
        }

        if (sinceDate <= 0) {
            Ln.e("Error fetching mentions, invalid sinceDate %s", String.valueOf(sinceDate));
            return ConversationContract.SyncOperationEntry.SyncState.FAULTED;
        }

        Ln.d("FetchMentionsOperation enqueued");
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        MentionsTask task = new MentionsTask(injector);

        task.setMaxActivities(MAX_ACTIVITIES);
        task.setSinceDate(sinceDate);
        task.execute();

        if (task.succeeded()) {
            settings.setHasLoadedMentions(true);
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

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == FETCH_MENTIONS;
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withExponentialBackoff();
    }
}
