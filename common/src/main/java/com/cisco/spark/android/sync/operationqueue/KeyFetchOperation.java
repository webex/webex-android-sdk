package com.cisco.spark.android.sync.operationqueue;

import android.database.Cursor;
import android.net.Uri;
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
import com.cisco.spark.android.util.UriUtils;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.EncryptionKeyEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for fetching a key from the KMS
 */
public class KeyFetchOperation extends PostKmsMessageOperation {

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient EncryptionDurationMetricManager encryptionDurationMetricManager;

    @Inject
    transient OperationQueue operationQueue;

    @Inject
    transient KeyManager keyManager;

    private final ArrayList<Uri> keyUris = new ArrayList<>();
    private final transient Object keyUrisLock = new Object();

    int totalKeys;

    public KeyFetchOperation(Injector injector, Collection<Uri> keyUris) {
        super(injector);
        this.keyUris.addAll(keyUris);
        totalKeys += keyUris.size();
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.FETCH_KEY;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        if (!keyManager.hasSharedKeyWithKMS()) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
        }

        return SyncState.READY;
    }

    @Override
    public String toString() {
        int remaining = keyUris == null ? 0 : keyUris.size();
        return new StringBuilder(super.toString())
                .append(" ")
                .append(totalKeys).append(" keys: ")
                .append(remaining == 0 ? "" : " " + remaining + " remaining")
                .toString();
    }

    /**
     * There's no point persisting key fetch operations across launches
     */
    @Override
    public boolean shouldPersist() {
        return false;
    }

    @NonNull
    @Override
    protected SyncState doWork() {
        loadKeysFromDb();

        if (checkProgress() == SyncState.SUCCEEDED)
            return SyncState.SUCCEEDED;

        String deviceId = getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            setErrorMessage("Failed to get Device ID");
            return SyncState.FAULTED;
        }

        ArrayList<PostKmsMessageOperation.EncryptedRequest> requests = new ArrayList<>();

        for (Uri keyUri : getKeyUrisCopy()) {
            try {
                String requestId = UUID.randomUUID().toString();
                encryptionDurationMetricManager.onKeyRequested(keyUri, System.currentTimeMillis());
                KmsRequest kmsRequest = CryptoUtils.getBoundKeyRequest(deviceId, apiTokenProvider.getAuthenticatedUser(), requestId, keyUri);
                if (kmsRequest == null) {
                    Ln.w("Failed generating key request message");
                    continue;
                }
                String encryptedBlob = kmsRequest.asEncryptedBlob(keyManager.getSharedKeyAsJWK());
                if (!TextUtils.isEmpty(encryptedBlob))
                    requests.add(new EncryptedRequest(kmsRequest, encryptedBlob, requestId));
            } catch (Exception e) {
                Ln.i(e, "Failed requesting key");
            }
        }

        if (requests.isEmpty()) {
            setErrorMessage("Key Request List is Empty");
            return SyncState.FAULTED;
        }

        sendKmsMessages(requests, KmsMessageRequestType.GET_KEYS);

        return SyncState.EXECUTING;
    }

    // Before requesting keys from the KMS make sure we don't have them in the DB already. If we do
    // then add them to the in-mem cache.
    protected void loadKeysFromDb() {

        List<Uri> keys;
        synchronized (keyUrisLock) {
            keys = new ArrayList<>(keyUris);
        }

        if (keys.isEmpty())
            return;

        // Sort our keys alphabetically by uri and request them from the DB the same way. Then
        // we can remove redundancy in a single pass.

        Collections.sort(keys);
        Uri compareKeyUri = keys.get(0);

        Cursor c = null;
        try {
            c = getContentResolver().query(EncryptionKeyEntry.CONTENT_URI,
                    EncryptionKeyEntry.DEFAULT_PROJECTION,
                    EncryptionKeyEntry.ENCRYPTION_KEY_URI + " >= ?",
                    new String[]{compareKeyUri.toString()},
                    EncryptionKeyEntry.ENCRYPTION_KEY_URI + " ASC");

            while (c != null && c.moveToNext() && !keys.isEmpty()) {
                String cursorKeyUri = c.getString(EncryptionKeyEntry.ENCRYPTION_KEY_URI.ordinal());
                compareKeyUri = keys.get(0);
                String keyValue = c.getString(EncryptionKeyEntry.ENCRYPTION_KEY.ordinal());

                if (compareKeyUri.toString().equals(cursorKeyUri) && !TextUtils.isEmpty(keyValue)) {
                    keyManager.addBoundKey(
                            compareKeyUri,
                            keyValue,
                            UriUtils.parseIfNotNull(c.getString(EncryptionKeyEntry.ENCRYPTION_KEY_ID.ordinal())));
                    synchronized (keyUrisLock) {
                        this.keyUris.remove(compareKeyUri);
                    }
                    keys.remove(0);
                } else {
                    if (cursorKeyUri.compareTo(compareKeyUri.toString()) > 0) {
                        keys.remove(0);
                    }
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    public boolean isOperationRedundant(Operation newOp) {
        if (newOp.getOperationType() != OperationType.FETCH_KEY)
            return false;

        KeyFetchOperation newKeyFetchOp = (KeyFetchOperation) newOp;

        // synchronized code block : Be careful to do very little in here, risk of deadlock with the shared lock
        synchronized (keyUrisLock) {
            Collection<Uri> ourKeyUris = getKeyUrisCopy();

            // This operation is still active so remove this operation's keys from the new operation's set.
            newKeyFetchOp.keyUris.removeAll(ourKeyUris);

            if (getState().isPreExecute()) {

                // If another FETCH_KEY operation is enqueued before this one starts, add its keys to this
                // operation and cancel the new one.

                keyUris.addAll(newKeyFetchOp.keyUris);
                totalKeys += newKeyFetchOp.keyUris.size();
            }
        }

        // check again before killing the new op, we're not synchronizing this and if the state went
        // to EXECUTING while we were adding uri's we could have missed some
        if (getState().isPreExecute()) {
            newKeyFetchOp.keyUris.removeAll(getKeyUrisCopy());
        }

        return newKeyFetchOp.keyUris.isEmpty();
    }

    @Override
    public SyncState checkProgress() {
        synchronized (keyUrisLock) {
            for (Uri keyUri : getKeyUrisCopy()) {
                if (keyManager.isBoundKeyCached(keyUri)) {
                    keyUris.remove(keyUri);
                }
            }
        }

        if (keyUris.isEmpty())
            return SyncState.SUCCEEDED;

        return SyncState.EXECUTING;
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        if (getState() == SyncState.FAULTED) {
            for (Uri keyUri : getKeyUrisCopy()) {
                encryptionDurationMetricManager.onKeyFetchFailed(keyUri);
            }
        }
    }

    public List<Uri> getKeyUrisCopy() {
        synchronized (keyUrisLock) {
            ArrayList<Uri> copy = new ArrayList<>();
            copy.addAll(keyUris);
            return Collections.unmodifiableList(copy);
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(2)
                .withRetryDelay(5, TimeUnit.SECONDS)
                .withAttemptTimeout(10, TimeUnit.SECONDS);
    }
}
