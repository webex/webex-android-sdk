package com.cisco.spark.android.sync.operationqueue;


import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.sync.KmsMessageRequestType;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.wx2.sdk.kms.KmsRequest;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for fetching unbound keys from the KMS
 */
public class UnboundKeyFetchOperation extends PostKmsMessageOperation {

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient KeyManager keyManager;

    @Inject
    transient OperationQueue operationQueue;

    @Inject
    transient EncryptionDurationMetricManager encryptionDurationMetricManager;

    private long keysToFetch;

    public UnboundKeyFetchOperation(Injector injector, long keysToFetch) {
        super(injector);
        this.keysToFetch = keysToFetch;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.FETCH_UNBOUND_KEYS;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        if (!keyManager.needMoreUnboundKeys())
            return SyncState.SUCCEEDED;

        return SyncState.READY;
    }

    @NonNull
    @Override
    public SyncState onPrepare() {
        if (!keyManager.needMoreUnboundKeys())
            return SyncState.SUCCEEDED;

        if (!keyManager.hasSharedKeyWithKMS()) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            return SyncState.PREPARING;
        }

        return super.onPrepare();
    }

    @NonNull
    @Override
    protected SyncState doWork() {
        if (!keyManager.needMoreUnboundKeys())
            return SyncState.SUCCEEDED;

        String deviceId = getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            setErrorMessage("Failed to get Device ID");
            return SyncState.FAULTED;
        }

        encryptionDurationMetricManager.onBeginFetchUnboundKeys(keysToFetch);

        String requestId = UUID.randomUUID().toString();
        KmsRequest kmsRequest = CryptoUtils.getUnboundKeyRequest(getDeviceId(), apiTokenProvider.getAuthenticatedUser(), requestId, (int) keysToFetch);
        if (kmsRequest == null) {
            setErrorMessage("Request for Unbound Keys is null");
            return SyncState.FAULTED;
        }

        String kmsMessage = kmsRequest.asEncryptedBlob(keyManager.getSharedKeyAsJWK());
        if (TextUtils.isEmpty(kmsMessage)) {
            setErrorMessage("Request Unbound Keys is empty");
            return SyncState.FAULTED;
        }

        sendKmsMessage(kmsMessage, requestId, KmsMessageRequestType.GET_KEYS, kmsRequest);
        return SyncState.EXECUTING;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    /**
     * Key fetches are async; after requesting via the conversation they (hopefully) arrive via
     * Mercury.
     *
     * @return SUCCEEDED if the keys have been populated. Otherwise no change.
     */
    @Override
    public SyncState checkProgress() {
        if (!keyManager.needMoreUnboundKeys())
            return SyncState.SUCCEEDED;

        return getState();
    }

    @Override
    public String toString() {
        return super.toString() + " (" + keysToFetch + ")";
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(5)
                .withRetryDelay(0)
                .withAttemptTimeout(10, TimeUnit.SECONDS);
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return getState().isPreExecute() && newOperation.getOperationType() == OperationType.FETCH_UNBOUND_KEYS;
    }

    /**
     * There's no point persisting key fetch operations across launches
     */
    @Override
    public boolean shouldPersist() {
        return false;
    }
}
