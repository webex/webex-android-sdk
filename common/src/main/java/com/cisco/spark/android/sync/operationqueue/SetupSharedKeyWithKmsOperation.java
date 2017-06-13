package com.cisco.spark.android.sync.operationqueue;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KmsInfo;
import com.cisco.spark.android.sync.KmsMessageRequestType;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class SetupSharedKeyWithKmsOperation extends PostKmsMessageOperation {

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient KeyManager keyManager;

    @Inject
    transient EncryptionDurationMetricManager encryptionDurationMetricManager;

    public SetupSharedKeyWithKmsOperation(Injector injector) {
        super(injector);
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.SET_UP_SHARED_KMS_KEY;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        if (keyManager.hasSharedKeyWithKMS())
            return SyncState.SUCCEEDED;

        KmsInfo kmsInfo = keyManager.getKmsInfo();
        if (kmsInfo == null) {
            Ln.w("Failed getting KMS Info!");
            return SyncState.READY;
        }

        Ln.i("Negotiating shared key with KMS");
        encryptionDurationMetricManager.onBeginSharedKeyNegotiation();

        String requestId = UUID.randomUUID().toString();
        KmsRequest kmsRequest = CryptoUtils.generateEphemeralKeyRequest(getDeviceId(), apiTokenProvider.getAuthenticatedUser(), kmsInfo.getKmsCluster(), requestId);
        if (kmsRequest == null) {
            setErrorMessage("Generate Ephemeral Key Request is null");
            return SyncState.FAULTED;
        }
        String encryptedRequest = kmsRequest.asEncryptedBlob(CryptoUtils.convertStringToJWK(kmsInfo.getRsaPublicKey()));
        if (TextUtils.isEmpty(encryptedRequest)) {
            setErrorMessage("Generate Ephemeral Key Request is empty");
            return SyncState.FAULTED;
        }
        sendKmsMessage(encryptedRequest, requestId, KmsMessageRequestType.CREATE_EPHEMERAL_KEY, kmsRequest);
        return SyncState.EXECUTING;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    protected SyncState checkProgress() {
        if (keyManager.hasSharedKeyWithKMS()) {
            return SyncState.SUCCEEDED;
        }
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(60, TimeUnit.MINUTES)
                .withExponentialBackoff()
                .withMaxAttempts(Integer.MAX_VALUE);
    }

    @Override
    protected String getDestination() {
        return Uri.withAppendedPath(keyManager.getKmsInfo().getKmsCluster(), "ecdhe").toString();
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @Override
    public boolean isOperationRedundant(Operation newOp) {
        return newOp.getOperationType() == OperationType.SET_UP_SHARED_KMS_KEY;
    }
}
