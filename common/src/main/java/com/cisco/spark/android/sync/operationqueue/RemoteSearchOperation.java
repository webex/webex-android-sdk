package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.ActivityDecryptedEvent;
import com.cisco.spark.android.model.Activity;
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
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.wx2.sdk.kms.KmsResource;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

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

    transient ArrayList<String> activitiesToDecrypt;
    private String searchString;

    private SearchStringWithModifiers searchStringWithModifiers;

    public RemoteSearchOperation(Injector injector, String searchString, SearchStringWithModifiers searchStringWithModifiers) {
        super(injector);
        this.searchString = searchString;
        this.searchStringWithModifiers = searchStringWithModifiers;
        this.activitiesToDecrypt = new ArrayList();
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
                    retrofit2.Response<ActivitySearchResponse> response = apiClientProvider.getSearchClient().querySearchService(remoteSearchQueryRequest).execute();
                    if (response.isSuccessful()) {
                        ActivitySearchResponse activitySearchResponse = response.body();
                        if (activitySearchResponse.getKmsMessage() != null) {
                            KmsResponseBody kmsResponseBody = CryptoUtils.decryptKmsMessage(activitySearchResponse.getKmsMessage(), keyManager.getSharedKeyAsJWK());
                            if (kmsResponseBody != null && kmsResponseBody.getResource() != null) {
                                KmsResource resource = kmsResponseBody.getResource();
                                searchKeyObject.setResources(UriUtils.toString(resource.getUri()));
                            }
                        }

                        for (Activity activity: activitySearchResponse.getActivities().getItems()) {
                            activitiesToDecrypt.add(activity.getId());
                        }

                        BulkActivitySyncTask searchResultActivitySyncTask = null;
                        if (activitySearchResponse.getActivities() != null && activitySearchResponse.getActivities().size() > 0) {
                            searchResultActivitySyncTask = new BulkActivitySyncTask(injector, activitySearchResponse.getActivities().getItems());
                            searchResultActivitySyncTask.execute();
                        }

                        if (activitySearchResponse.getActivities().size() == 0) {
                            return SyncOperationEntry.SyncState.SUCCEEDED;
                        }

                        bus.register(this);

                        return SyncOperationEntry.SyncState.EXECUTING;
                    } else {
                        String message = CryptoUtils.getKmsErrorMessage(response, gson, keyManager.getSharedKeyAsJWK());
                        if (!TextUtils.isEmpty(message)) {
                            Ln.w("KMS ERROR while binding key to search query: " + message + " expired?" + searchKeyObject.isKeyExpired() + " | " + LoggingUtils.toString(response));
                        }
                        return SyncOperationEntry.SyncState.FAULTED;
                    }
                } catch (IOException e) {
                    Ln.e(e);
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

        if (activitiesToDecrypt.size() > 0) {
            return SyncOperationEntry.SyncState.EXECUTING;
        }

        return SyncOperationEntry.SyncState.SUCCEEDED;
    }

    @Override
    protected void onStateChanged(SyncOperationEntry.SyncState oldState) {
        if (getState() == SyncOperationEntry.SyncState.SUCCEEDED) {
            bus.post(new RemoteSearchOperationCompletedEvent(this));
        }

        if (getState().isTerminal() && bus.isRegistered(this)) {
            bus.unregister(this);
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

    public void onEvent(ActivityDecryptedEvent event) {
        Ln.d("Got Decrypted Activities");
        ArrayList<Activity> decryptedActivities = new ArrayList<>();
        for (Activity activity : event.getActivities()) {
            if (activitiesToDecrypt.remove(activity.getId())) {
                decryptedActivities.add(activity);
            }
        }

        if (decryptedActivities.size() > 0) {
            conversationSearchManager.updateActivitySearchSync(decryptedActivities);
            bus.post(new RemoteSearchOperationCompletedEvent(this));
        }
    }
}
