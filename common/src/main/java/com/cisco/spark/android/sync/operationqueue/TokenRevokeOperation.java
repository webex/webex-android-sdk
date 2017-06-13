package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class TokenRevokeOperation extends Operation {

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient OAuth2 oAuth2;

    private final String token;

    public TokenRevokeOperation(Injector injector, String token) {
        super(injector);
        this.token = token;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.REVOKE_TOKEN;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.SyncState onPrepare() {
        if (!apiTokenProvider.isAuthenticated())
            return ConversationContract.SyncOperationEntry.SyncState.PREPARING;

        return super.onPrepare();
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        if (!TextUtils.isEmpty(token)) {
            oAuth2.revokeAccessToken(apiClientProvider.getOAuthClient(), token);
        }

        return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == getOperationType()
                && TextUtils.equals(token, ((TokenRevokeOperation) newOperation).token);
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (isOperationRedundant(newOperation))
            newOperation.cancel();
    }

    @Override
    protected ConversationContract.SyncOperationEntry.SyncState checkProgress() {
        return getState();
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withInitialDelay(30, TimeUnit.SECONDS)
                .withAttemptTimeout(10, TimeUnit.SECONDS);
    }
}
