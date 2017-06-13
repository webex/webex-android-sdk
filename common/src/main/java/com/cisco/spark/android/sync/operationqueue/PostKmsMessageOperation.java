package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.client.VoidCallback;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KmsRequestResponseComplete;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.KmsMessageRequestType;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.github.benoitdion.ln.Ln;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.SET_UP_SHARED_KMS_KEY;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState.PREPARING;

public abstract class PostKmsMessageOperation extends Operation {
    @Inject
    transient KeyManager keyManager;
    @Inject
    transient ApiClientProvider apiClientProvider;
    @Inject
    transient DeviceRegistration deviceRegistration;
    @Inject
    transient MercuryClient mercuryClient;
    @Inject
    transient OperationQueue operationQueue;

    public PostKmsMessageOperation(Injector injector) {
        super(injector);
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.SyncState onPrepare() {
        if (!mercuryClient.isRunning()) {
            Ln.d("Deferring operation because Mercury is not running");
            return SyncState.PREPARING;
        }

        if (getOperationType() != SET_UP_SHARED_KMS_KEY
                && !keyManager.hasSharedKeyWithKMS()) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            return PREPARING;
        }
        return super.onPrepare();
    }

    protected void sendKmsMessages(List<EncryptedRequest> encryptedRequests, KmsMessageRequestType type) {
        KmsRequestResponseComplete kmsRequestComplete = new KmsRequestResponseComplete();
        for (EncryptedRequest request : encryptedRequests) {
            keyManager.addKmsMessageRequest(request.requestId, type, request.kmsRequest);
            kmsRequestComplete.addKmsMessage(request.encryptedRequest);
        }

        kmsRequestComplete.setDestination(getDestination());

        if (kmsRequestComplete.getKmsMessages() != null && !kmsRequestComplete.getKmsMessages().isEmpty()) {
            apiClientProvider.getSecurityClient().postKmsMessage(kmsRequestComplete, new VoidCallback());
        }
    }

    protected void sendKmsMessage(String encryptedRequest, String requestId, KmsMessageRequestType type, KmsRequest kmsRequest) {
        keyManager.addKmsMessageRequest(requestId, type, kmsRequest);
        KmsRequestResponseComplete kmsRequestComplete = new KmsRequestResponseComplete();
        kmsRequestComplete.addKmsMessage(encryptedRequest);
        kmsRequestComplete.setDestination(getDestination());
        if (kmsRequestComplete.getKmsMessages() != null && !kmsRequestComplete.getKmsMessages().isEmpty()) {
            apiClientProvider.getSecurityClient().postKmsMessage(kmsRequestComplete, new VoidCallback());
        }
    }

    protected String getDestination() {
        return keyManager.getKmsInfo().getKmsCluster().toString();
    }

    protected String getDeviceId() {
        return UriUtils.toString(deviceRegistration.getUrl());
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(3)
                .withAttemptTimeout(30, TimeUnit.SECONDS);
    }

    public static class EncryptedRequest {
        public EncryptedRequest(KmsRequest kmsRequest, String encryptedRequest, String requestId) {
            this.kmsRequest = kmsRequest;
            this.encryptedRequest = encryptedRequest;
            this.requestId = requestId;
        }

        KmsRequest kmsRequest;
        String encryptedRequest;
        String requestId;
    }
}
