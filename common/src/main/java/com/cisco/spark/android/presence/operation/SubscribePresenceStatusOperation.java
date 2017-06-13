package com.cisco.spark.android.presence.operation;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.presence.PresenceStatusListener;
import com.cisco.spark.android.presence.PresenceStatusRequest;
import com.cisco.spark.android.presence.PresenceSubscriptionResponse;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

public class SubscribePresenceStatusOperation extends Operation {
    private String subjectUuid;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient PresenceStatusListener presenceStatusCache;

    public SubscribePresenceStatusOperation(Injector injector, String subjectUuid) {
        super(injector);
        this.subjectUuid = subjectUuid;
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

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.SUSBSCRIBE_USER_PRESENCE;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        ArrayList<String> subjects = new ArrayList<String>();
        subjects.add(subjectUuid);
        PresenceStatusRequest request = new PresenceStatusRequest(subjects, 10, TimeUnit.MINUTES);

        Response<PresenceSubscriptionResponse> response = apiClientProvider.getPresenceClient().postSubscribeTo(request).execute();

        if (response.isSuccessful()) {
            List<PresenceSubscriptionResponse.Response> responses = response.body().getResponses();
            for (PresenceSubscriptionResponse.Response subscription : responses) {
                if (subscription.subject.equals(subjectUuid)) {
                    if (subscription.getResponseCode() == 409) {
                        // NOTE: 409 usually indicates that the client can not accept any more subscriptions.
                        // Retry in a while based on the servers retry response.
                        Ln.d("Error subscribing to subjects presence updates, retrying");
                        RetryPolicy policy = getRetryPolicy();
                        policy.setRetryDelay(subscription.getRetry(), TimeUnit.SECONDS);
                        return SyncState.READY;
                    } else if (subscription.getResponseCode() == 403) {
                        // NOTE: 403 indicates either you or the person you are subscribing to does not
                        // have the developer feature for presence. Retry subscribing until such point
                        // this operation runs out of retries.
                        return SyncState.READY;
                    } else if (subscription.getResponseCode() == 401) {
                        // NOTE: 401 you do not have access to this subjects subscription.
                        Ln.d("Error subscribing to subjects presence updates, Not Authorized");
                    } else {
                        presenceStatusCache.addSubscription(subscription.getSubject(), subscription.getSubscriptionTtl());
                    }
                }
            }
            return SyncState.SUCCEEDED;
        }
        return SyncState.READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != OperationType.SUSBSCRIBE_USER_PRESENCE) {
            return false;
        }

        SubscribePresenceStatusOperation subscribePresenceStatusOperation = (SubscribePresenceStatusOperation) newOperation;

        return subjectUuid.equals(subscribePresenceStatusOperation.getSubjectUuid());
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    public String getSubjectUuid() {
        return subjectUuid;
    }
}
