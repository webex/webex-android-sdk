package com.cisco.spark.android.sync.operationqueue;


import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.RetentionPolicyInfoEvent;
import com.cisco.spark.android.model.RetentionPolicy;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

public class GetRetentionPolicyInfoOperation extends Operation {
    @Inject
    transient EventBus bus;
    @Inject
    transient ApiClientProvider apiClientProvider;

    private String custodianOrgId;
    private Uri retentionUrl;
    private boolean isOneOnOneConversation;
    private String conversationId;
    private RetentionPolicy retentionPolicy;
    private Context context;


    public GetRetentionPolicyInfoOperation(Injector injector, Context context, String custodianOrgId, Uri retentionUrl, String conversationId, boolean isOneOnOneConversation) {
        super(injector);
        this.custodianOrgId = custodianOrgId;
        this.retentionUrl = retentionUrl;
        this.conversationId = conversationId;
        this.isOneOnOneConversation = isOneOnOneConversation;
        this.context = context;
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

    @Override
    public boolean isOperationRedundant(Operation newOp) {
        if (newOp.getOperationType() != ConversationContract.SyncOperationEntry.OperationType.GET_RETENTION_POLICY)
            return false;

        return ((GetRetentionPolicyInfoOperation) newOp).getConversationId().equals(getConversationId());
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        Response<RetentionPolicy> response = null;
        if (isOneOnOneConversation) {
            response = apiClientProvider.getConversationClient().getRetentionPolicyForOneOnOneConversation().execute();
        } else {
            response = apiClientProvider.getConversationClient().getRetentionPolicyForGroupConversation(custodianOrgId).execute();
        }
        if (response.isSuccessful()) {
            retentionPolicy = response.body();
            writeToDatabase(conversationId, retentionPolicy);
            return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;
        } else {
            Ln.w("Failed getting retention policy duration " + LoggingUtils.toString(response));
            return ConversationContract.SyncOperationEntry.SyncState.FAULTED;
        }
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
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(10, TimeUnit.SECONDS)
                .withRetryDelay(10)
                .withMaxAttempts(2);
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    private void writeToDatabase(String conversationId, RetentionPolicy retentionPolicy) {
        if (retentionPolicy != null) {
            String retentionDaysAsString = retentionPolicy.getRetentionDurationInfo(context);
            Batch batch = newBatch();
            batch.add(
                    ContentProviderOperation.newUpdate(Uri.withAppendedPath(ConversationContract.ConversationEntry.CONTENT_URI, conversationId))
                            .withValue(ConversationContract.ConversationEntry.RETENTION_DAYS.name(), retentionDaysAsString)
                            .build());
            batch.apply();
        }
    }

    public String getConversationId() {
        return conversationId;
    }
}
