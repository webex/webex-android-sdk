package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.ActivitySearchResponse;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.RemoteSearchQueryRequest;
import com.cisco.spark.android.model.SearchStringWithModifiers;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.SearchManager;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.ActivitySyncQueue;
import com.cisco.spark.android.sync.queue.BulkActivitySyncTask;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.wx2.sdk.kms.KmsResource;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class RemoteSearchOperation extends Operation {
    @Inject
    transient SearchManager conversationSearchManager;

    @Inject
    transient KeyManager keyManager;

    @Inject
    transient ActivitySyncQueue activitySyncQueue;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient Gson gson;

    @Inject
    transient OperationQueue operationQueue;

    @Inject
    transient EventBus bus;

    private String searchString;

    private SearchStringWithModifiers searchStringWithModifiers;

    public RemoteSearchOperation(Injector injector, String searchString, SearchStringWithModifiers searchStringWithModifiers) {
        super(injector);
        this.searchString = searchString;
        this.searchStringWithModifiers = searchStringWithModifiers;
    }

    @NonNull
    @Override
    public SyncOperationEntry.OperationType getOperationType() {
        return SyncOperationEntry.OperationType.REMOTE_SEARCH_QUERY;
    }

    @NonNull
    @Override
    protected SyncOperationEntry.SyncState onEnqueue() {
        return SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    public SyncOperationEntry.SyncState onPrepare() {
        Ln.d("onPrepare: RemoteSearchOperation for searchString " + searchString);
        if (!keyManager.hasSharedKeyWithKMS()) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            return SyncOperationEntry.SyncState.PREPARING;
        }
        return super.onPrepare();
    }

    private void prepareRemoteSearchRequest(RemoteSearchQueryRequest remoteSearchQueryRequest, KeyObject searchKeyObject) {
        if (searchStringWithModifiers != null && searchStringWithModifiers.usesModifiers()) {
            String uuid = searchStringWithModifiers.getFromModifierUUID();
            if (!TextUtils.isEmpty(uuid)) {
                remoteSearchQueryRequest.setSharedBy(uuid);
            }
            uuid = searchStringWithModifiers.getInModifierUUID();
            if (!TextUtils.isEmpty(uuid)) {
                remoteSearchQueryRequest.setSharedIn(uuid);
            }
            if (!searchStringWithModifiers.getWithModifierUUID().isEmpty()) {
                remoteSearchQueryRequest.setSharedWith(searchStringWithModifiers.getWithModifierUUID());
            }
            if (!TextUtils.isEmpty(searchStringWithModifiers.getSearchStringWithNoModifier())) {
                remoteSearchQueryRequest.setQuery(CryptoUtils.encryptAES(searchStringWithModifiers.getSearchStringWithNoModifier(), searchKeyObject.getKey()));
            } else {
                remoteSearchQueryRequest.setQuery(null);
            }
        }
    }

    @NonNull
    @Override
    protected SyncOperationEntry.SyncState doWork() throws IOException {
        Ln.d("Do work RemoteSearchOperation for searchString " + searchString);
        if ((conversationSearchManager.isMessageSearchEnabled() || conversationSearchManager.isContentSearchEnabled()) && keyManager.getSharedKeyWithKMS() != null) {
            KeyObject searchKeyObject = keyManager.getSearchKey();
            if (searchKeyObject != null && !TextUtils.isEmpty(searchKeyObject.getKey())) {
                try {
                    String encryptedQueryString = CryptoUtils.encryptAES(searchString, searchKeyObject.getKey());
                    RemoteSearchQueryRequest remoteSearchQueryRequest = new RemoteSearchQueryRequest(encryptedQueryString, searchKeyObject.getKeyUrl());
                    if (searchKeyObject.getKmsResourceObject() == null) {
                        String searchKmsMessage = conversationProcessor.createNewResource(null, Collections.singletonList(searchKeyObject.getKeyUrl()));
                        remoteSearchQueryRequest.setKmsMessage(searchKmsMessage);
                    }
                    prepareRemoteSearchRequest(remoteSearchQueryRequest, searchKeyObject);
                    ActivitySearchResponse searchResponse = apiClientProvider.getSearchClient().querySearchService(remoteSearchQueryRequest);
                    if (searchResponse != null) {
                        if (searchResponse.getKmsMessage() != null) {
                            KmsResponseBody kmsResponseBody = CryptoUtils.decryptKmsMessage(searchResponse.getKmsMessage(), keyManager.getSharedKeyAsJWK());
                            if (kmsResponseBody != null && kmsResponseBody.getResource() != null) {
                                KmsResource resource = kmsResponseBody.getResource();
                                searchKeyObject.setResources(UriUtils.toString(resource.getUri()));
                            }
                        }
                        BulkActivitySyncTask searchResultActivitySyncTask = null;
                        if (searchResponse.getActivities() != null && searchResponse.getActivities().size() > 0) {
                            searchResultActivitySyncTask = new BulkActivitySyncTask(injector, searchResponse.getActivities().getItems());
                            searchResultActivitySyncTask.execute();
                        }
                        if (!this.isCanceled() && searchResultActivitySyncTask != null && searchResultActivitySyncTask.hasSyncSucceeded()) {
                            bus.post(new RemoteSearchOperationCompletedEvent(this));
                        }
                        return SyncOperationEntry.SyncState.SUCCEEDED;
                    }
                } catch (RetrofitError error) {
                    Response response = error.getResponse();
                    if (response != null && response.getStatus() >= 400 && response.getStatus() < 500) {
                        String message = CryptoUtils.getKmsErrorMessage(error, gson, keyManager.getSharedKeyAsJWK());
                        if (!TextUtils.isEmpty(message)) {
                            Ln.w("KMS ERROR while binding key to search query: " + message + " expired?" + searchKeyObject.isKeyExpired());
                        }
                    }
                    return SyncOperationEntry.SyncState.FAULTED;
                }
            }
        } else {
            Ln.w("Unable to query search service - message search enabled " + conversationSearchManager.isMessageSearchEnabled() + " , Shared key with KMS exists " + keyManager.hasSharedKeyWithKMS());
        }
        return SyncOperationEntry.SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (newOperation.getOperationType() != SyncOperationEntry.OperationType.REMOTE_SEARCH_QUERY)
            return;
        if (((RemoteSearchOperation) newOperation).getSearchString().equals(searchString)) {
            cancel();
        }
    }

    public String getSearchString() {
        return searchString;
    }

    @Override
    protected SyncOperationEntry.SyncState checkProgress() {
        return getState();
    }

    @Override
    protected void onStateChanged(SyncOperationEntry.SyncState oldState) {
        if (getState() == SyncOperationEntry.SyncState.SUCCEEDED) {
            bus.post(new RemoteSearchOperationCompletedEvent(this));
        }
    }

    /**
     * No retries necessary
     */
    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(10, TimeUnit.SECONDS)
                .withRetryDelay(0)
                .withMaxAttempts(3);
    }

    /**
     * Operation should move to fail if there is no network
     */
    @Override
    public boolean needsNetwork() {
        return false;
    }

    /**
     * Remote search operations need not be persisted across launches
     */
    @Override
    public boolean shouldPersist() {
        return false;
    }

    public static class RemoteSearchOperationCompletedEvent {
        public RemoteSearchOperation operation;

        private RemoteSearchOperationCompletedEvent(RemoteSearchOperation op) {
            this.operation = op;
        }
    }
}
