package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.client.EmptyBody;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncStateFailureReason;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;


/* NOTE:
 * This class maps the X-Prelogin-UserId header to a users actually User ID in mix panel so that we can follow the
 * entire onboarding process from start to finish.
 */
public class AliasPreloginMetricsUserIdOperation extends Operation {
    private String preloginId;

    @Inject
    transient ApiClientProvider apiClientProvider;

    public AliasPreloginMetricsUserIdOperation(Injector injector, String preloginId) {
        super(injector);

        this.preloginId = preloginId;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == OperationType.POST_ALIAS_USER && ((AliasPreloginMetricsUserIdOperation) newOperation).preloginId == preloginId;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.POST_ALIAS_USER;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        Response<Void> response = apiClientProvider.getMetricsClient().postUserAlias(this.preloginId, new EmptyBody()).execute();

        if (response.isSuccessful()) {
            return SyncState.SUCCEEDED;
        }

        if (response.code() >= 500) {
            return SyncState.READY;
        } else {
            setFailureReason(SyncStateFailureReason.INVALID_RESPONSE);
            setErrorMessage(response.errorBody().string());

            return SyncState.FAULTED;
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }
}
