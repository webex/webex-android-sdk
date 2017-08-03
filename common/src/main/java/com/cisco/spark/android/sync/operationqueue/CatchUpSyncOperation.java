package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.Timing;
import com.cisco.spark.android.metrics.TimingProvider;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.CatchUpSyncTask;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.sync.queue.IncrementalSyncTask;
import com.cisco.spark.android.sync.queue.ShellsTask;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.HashMap;
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

    @Inject
    transient TimingProvider timingProvider;

    @Inject
    transient DeviceRegistration deviceRegistration;


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
        long highWaterMark = ConversationSyncQueue.getHighWaterMark(getContentResolver());
        if (highWaterMark == 0) {
            success = doInitialSync();
        } else {
            success = doCatchUpSync(System.currentTimeMillis() - highWaterMark > TimeUnit.HOURS.toMillis(12));
        }

        return success
                ? ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED
                : ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    private boolean doCatchUpSync(boolean includeFastShells) {
        Ln.i("Starting Catch-up Sync");
        Timing t = timingProvider.get("android_performance_conversations_update");
        t.start();
        bus.post(new ConversationSyncQueue.ConversationSyncStartedEvent());

        // Be nice to the server, don't bother with fast shells unless it has been a while.
        if (includeFastShells) {
            getFastShellsTask()
                    .withMaxParticipants(0)
                    .execute();
            bus.post(new ConversationSyncQueue.ConversationListCompletedEvent());
        }

        CatchUpSyncTask catchUpSyncTask = new CatchUpSyncTask(injector);
        boolean success = catchUpSyncTask.execute();

        if (success) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("count", catchUpSyncTask.getConversationsProcessed());
            map.put("buffered-mercury-enabled", deviceRegistration.getFeatures().isBufferedMercuryEnabled());
            map.put("include-fast-shells", includeFastShells);
            t.endAndPublish(map);
        }

        if (!includeFastShells)
            bus.post(new ConversationSyncQueue.ConversationListCompletedEvent());

        return success;
    }

    private boolean doInitialSync() {
        Ln.i("Starting Initial Sync");
        Timing t = timingProvider.get("android_performance_conversations_initial");
        t.start();

        bus.post(new ConversationSyncQueue.ConversationSyncStartedEvent());

        getFastShellsTask()
                // Min participants to render the title, plus one
                .withMaxParticipants(ConversationResolver.MAX_TOP_PARTICIPANTS + 1)
                .execute();

        t.addSplit("fastShells");

        // This gets all the convs, all the participants, and 1 activity
        boolean success = getFullShellsTask().execute();

        t.addSplit("fullShells");

        if (success) {
            bus.post(new ConversationSyncQueue.ConversationListCompletedEvent());
            CatchUpSyncTask catchUpSyncTask = new CatchUpSyncTask(injector);
            success = catchUpSyncTask.execute();

            HashMap<String, Object> map = new HashMap<>();
            map.put("count", catchUpSyncTask.getConversationsProcessed());

            if (success) t.endAndPublish(map);
        }

        if (success) t.endAndPublish();

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
        return RetryPolicy.newJobTimeoutPolicy(3, TimeUnit.HOURS)
                .withExponentialBackoff(5, 120, TimeUnit.SECONDS)
                .withAttemptTimeout(30, TimeUnit.MINUTES);
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
