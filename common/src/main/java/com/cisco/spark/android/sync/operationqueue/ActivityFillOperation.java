package com.cisco.spark.android.sync.operationqueue;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.ConversationActivityFillTask;
import com.cisco.spark.android.sync.queue.ConversationBackFillTask;
import com.cisco.spark.android.sync.queue.ConversationForwardFillTask;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.ACTIVITY_FILL;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class ActivityFillOperation extends Operation implements ConversationOperation {
    private static final int MAX_ACTIVITIES = 50;

    private String conversationId;
    private Uri gapUri;
    private boolean isBackfill; // True if we're backfilling, false otherwise. It's assumed that we're
                                // forward filling if we're not backfilling. If we add more fill types
                                // in the future this whill have to be changed to an enum

    public ActivityFillOperation(Injector injector, String conversationId, Uri gapUri) {
        super(injector);
        this.conversationId = conversationId;
        this.gapUri = gapUri;
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ACTIVITY_FILL;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        String gapType = ConversationContentProviderQueries.getGapType(getContentResolver(), gapUri);

        if (gapType == null) {
            Ln.e("Error filling conversation gap, no gap found at " + gapUri.toString());
            cancel();
            return SyncState.FAULTED;
        }

        Ln.d("ActivityFillOperation enqueued, gap type is " + gapType);
        isBackfill = ConversationContract.ActivityEntry.Type.BACKFILL_GAP.name().equals(gapType);
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {

        if (null == ConversationContentProviderQueries.getGapType(getContentResolver(), gapUri)) {
            // the gap uri is gone, something else must have filled it in
            Ln.i("Error filling conversation gap, gap no longer exists at " + gapUri.toString());
            return SyncState.FAULTED;
        }

        ConversationActivityFillTask task;
        if (isBackfill) {
            task = new ConversationBackFillTask(injector);
        } else {
            task = new ConversationForwardFillTask(injector);
        }
        task.setMaxActivities(MAX_ACTIVITIES);
        task.setGap(gapUri);
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
    protected SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withJobTimeout(TimeUnit.MINUTES.toMillis(5))
                .withExponentialBackoff(1, 10, TimeUnit.SECONDS);
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == ACTIVITY_FILL && conversationId.equals(((ActivityFillOperation) newOperation).getConversationId());
    }
}
