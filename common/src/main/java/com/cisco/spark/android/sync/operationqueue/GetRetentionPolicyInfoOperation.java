package com.cisco.spark.android.sync.operationqueue;


import android.content.ContentProviderOperation;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.RetentionPolicyInfoEvent;
import com.cisco.spark.android.model.RetentionPolicy;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

public class GetRetentionPolicyInfoOperation extends Operation {
    @Inject
    transient EventBus bus;
    @Inject
    transient ApiClientProvider apiClientProvider;

    private String retentionUrl;

    private RetentionPolicy retentionPolicy;

    public GetRetentionPolicyInfoOperation(Injector injector, String retentionUrl) {
        super(injector);
        this.retentionUrl = retentionUrl;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        // get retention policy from local first since the retention policy is same for one org at most time.
        retentionPolicy = ConversationContentProviderQueries.getRetentionPolicyOfRetetentionUrl(getContentResolver(), retentionUrl);
        if (retentionPolicy != null && !DateUtils.isTimestampOverdue(retentionPolicy.getLastRetentionSyncTimestamp(), DateUtils.DAY_IN_MILLIS)) {
            updateRetentionPolicy(retentionPolicy);
            return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;
        }

        Response<RetentionPolicy> response = apiClientProvider.getConversationClient().getRetentionPolicy(retentionUrl).execute();

        if (response.isSuccessful()) {
            retentionPolicy = response.body();
        } else if (HTTP_NOT_FOUND == response.code()) {
            // conversation service team said they will return 404 for those orgs who are not on configure list.
            Ln.i("Retention policy undefined");
            retentionPolicy = new RetentionPolicy();
            retentionPolicy.setRetentionDays(-1);
            retentionPolicy.setLastRetentionSyncTimestamp(System.currentTimeMillis());
        }

        if (retentionPolicy == null) {
            Ln.w("Failed getting retention policy duration " + LoggingUtils.toString(response));
            return ConversationContract.SyncOperationEntry.SyncState.FAULTED;
        } else {
            updateRetentionPolicy(retentionPolicy);
            return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;
        }
    }

    @Override
    public boolean isOperationRedundant(Operation newOp) {
        if (newOp.getOperationType() != ConversationContract.SyncOperationEntry.OperationType.GET_RETENTION_POLICY) {
            return false;
        }

        if (((GetRetentionPolicyInfoOperation) newOp).getRetentionUrl().equals(getRetentionUrl())) {
            return true;
        }

        return false;
    }

    public String getRetentionUrl() {
        return Strings.isEmpty(retentionUrl) ? "" : retentionUrl;
    }

    @Override
    protected void onStateChanged(ConversationContract.SyncOperationEntry.SyncState oldState) {
        super.onStateChanged(oldState);
        if (getState().isTerminal()) {
            bus.post(new RetentionPolicyInfoEvent(retentionPolicy));
        }
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.GET_RETENTION_POLICY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(10, TimeUnit.SECONDS)
                .withRetryDelay(10)
                .withMaxAttempts(2);
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    private void updateRetentionPolicy(@NonNull RetentionPolicy retentionPolicy) {
        Batch batch = newBatch();
        batch.add(
                ContentProviderOperation.newUpdate(ConversationContract.ConversationEntry.CONTENT_URI)
                        .withValue(ConversationContract.ConversationEntry.RETENTION_DAYS.name(), retentionPolicy.getRetentionDays())
                        .withValue(ConversationContract.ConversationEntry.LAST_RETENTION_SYNC_TIMESTAMP.name(), System.currentTimeMillis())
                        .withSelection(ConversationContract.ConversationEntry.RETENTION_URL + "=?", new String[]{retentionUrl})
                        .build());
        batch.apply();
    }
}
