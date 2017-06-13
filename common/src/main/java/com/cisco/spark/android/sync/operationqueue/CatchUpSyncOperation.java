package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.CatchUpSyncTask;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.sync.queue.IncrementalSyncTask;
import com.cisco.spark.android.sync.queue.ShellsTask;
import com.cisco.spark.android.ui.conversation.ConversationResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.FETCH_MENTIONS;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.GENERAL_SYNC_TASK;


/**
 * Used for initial sync and catch-ups
 */
public class CatchUpSyncOperation extends Operation {

    @Inject
    transient EventBus bus;

    public CatchUpSyncOperation(Injector injector) {
        super(injector);
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.INCREMENTAL_SYNC;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {

        boolean success;
        if (ConversationSyncQueue.getHighWaterMark(getContentResolver()) == 0) {
            success = doInitialSync();
        } else {
            success = doCatchUpSync();
        }

        return success
                ? ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED
                : ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    private boolean doCatchUpSync() {
        getFastShellsTask()
                .withMaxParticipants(0)
                .execute();

        bus.post(new ConversationSyncQueue.ConversationListCompletedEvent());
        return new CatchUpSyncTask(injector).execute();
    }

    private boolean doInitialSync() {
        boolean success;
        getFastShellsTask()
                // Min participants to render the title, plus one
                .withMaxParticipants(ConversationResolver.MAX_TOP_PARTICIPANTS + 1)
                .execute();

        // This gets all the convs, all the participants, and 1 activity
        success = getFullShellsTask().execute();

        if (success) {
            bus.post(new ConversationSyncQueue.ConversationListCompletedEvent());
            success = new CatchUpSyncTask(injector).execute();
        }
        return success;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == getOperationType();
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (newOperation.getOperationType() == GENERAL_SYNC_TASK
                || newOperation.getOperationType() == FETCH_MENTIONS)
            newOperation.setDependsOn(this);
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(1, TimeUnit.HOURS)
                .withExponentialBackoff(5, 120, TimeUnit.MINUTES)
                .withAttemptTimeout(10, TimeUnit.MINUTES);
    }

    public ShellsTask getFastShellsTask() {
        IncrementalSyncTask ret = new ShellsTask(injector)
                .withMaxConversations(IncrementalSyncTask.MAX_CONVERSATIONS_SHELL)
                .withSinceTime(IncrementalSyncTask.HIGH_WATER_MARK);

        return (ShellsTask) ret;
    }

    private ShellsTask getFullShellsTask() {
        IncrementalSyncTask ret = new ShellsTask(injector)
                .withMaxParticipants(IncrementalSyncTask.MAX_PARTICIPANTS)
                .withMaxConversations(IncrementalSyncTask.MAX_CONVERSATIONS)
                .withSinceTime(IncrementalSyncTask.HIGH_WATER_MARK);

        return (ShellsTask) ret;
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }
}
