package com.cisco.spark.android.presence.operation;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.presence.PresenceEvent;
import com.cisco.spark.android.presence.PresenceEventResponse;
import com.cisco.spark.android.presence.PresenceStatus;
import com.cisco.spark.android.presence.PresenceUtils;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

public class SendPresenceEventOperation extends Operation {
    PresenceEvent event;

    @Inject
    transient AuthenticatedUserProvider authenticatedUserProvider;

    @Inject
    transient ActorRecordProvider actorRecordProvider;

    @Inject
    transient ApiClientProvider apiClientProvider;

    public SendPresenceEventOperation(Injector injector, PresenceStatus status, int ttlInSeconds) {
        super(injector);
        event = new PresenceEvent(authenticatedUserProvider.getAuthenticatedUser().getUserId(), status, ttlInSeconds, null);
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.SEND_PRESENCE_EVENT;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        Batch batch = newBatch();
        ActorRecord actorRecord = actorRecordProvider.get(event.getSubject());

        actorRecord.setPresenceStatus(event.getEventType());
        actorRecord.setPresenceExpiration(PresenceUtils.getExpireTime(event.getTtl()));

        actorRecord.addInsertUpdateContentProviderOperation(batch);

        batch.apply();

        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        Response<PresenceEventResponse> response  = apiClientProvider.getPresenceClient().postPresenceEvent(event).execute();

        if (response.isSuccessful()) {
            return SyncState.SUCCEEDED;
        } else {
            return SyncState.READY;
        }
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != OperationType.SEND_PRESENCE_EVENT) {
            return false;
        }

        SendPresenceEventOperation sendPresenceEventOperation = (SendPresenceEventOperation) newOperation;

        if (TextUtils.equals(event.getSubject(), sendPresenceEventOperation.getEvent().getSubject())) {
            cancel();
        }

        return false;
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }

    protected PresenceEvent getEvent() {
        return this.event;
    }
}
