package com.cisco.spark.android.sync.operationqueue;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.lyra.BindingRequest;
import com.cisco.spark.android.lyra.BindingResponse;
import com.cisco.spark.android.lyra.LyraService;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;

public class RoomBindOperation extends Operation {

    private Uri conversationUrl;
    private String roomIdentity;
    private String conversationId;

    private Uri keyUri;
    private Response<BindingResponse> response;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient LyraService lyraService;

    public RoomBindOperation(Injector injector, Uri conversationUrl, String roomIdentity, String conversationId) {
        super(injector);
        this.conversationUrl = conversationUrl;
        this.roomIdentity = roomIdentity;
        this.conversationId = conversationId;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.ROOM_BIND;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    @CallSuper
    public SyncState onPrepare() {
        SyncState ret = super.onPrepare();

        if (ret != SyncState.READY)
            return ret;

        Bundle convValues = ConversationContentProviderQueries.getConversationById(getContentResolver(), conversationId);
        if (convValues == null) {
            Ln.i("Conversation " + conversationId + " does not exist locally yet");
            return SyncState.PREPARING;
        }

        keyUri = UriUtils.parseIfNotNull(convValues.getString(DEFAULT_ENCRYPTION_KEY_URL.name()));
        if (keyUri == null) {
            Ln.i("Not ready, conversation has a blank key uri.");
            Operation updateKeyOperation = operationQueue.updateConversationKey(conversationId);
            setDependsOn(updateKeyOperation);
            ret = SyncState.PREPARING;
        }

        return ret;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        String kmsMessage = getAddKmsMessage(roomIdentity, conversationId);
        response = apiClientProvider.getLyraClient().bind(roomIdentity, new BindingRequest(conversationUrl, kmsMessage)).execute();

        if (!response.isSuccessful()) {
            return SyncState.READY;
        }

        lyraService.onBindingOperationSucceeded(response.message());
        return SyncState.SUCCEEDED;
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        if (getState() == SyncState.FAULTED) {
            lyraService.onBindingOperationFailed(response == null ? -1 : response.code(), response == null ? null : response.message());
        }
    }


    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);
        if (newOperation.getOperationType() == OperationType.ROOM_BIND) {
            Ln.d("Canceling because a newer Room binding operation was just posted. " + this);
            cancel();
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(1);
    }

    private  String getAddKmsMessage(String roomIdentity, String conversationId) {
        Set<String> userIds = new HashSet<>();
        userIds.add(roomIdentity);
        KmsResourceObject kro = ConversationContentProviderQueries.getKmsResourceObject(
                getContentResolver(), conversationId);
        return conversationProcessor.authorizeNewParticipantsUsingKmsMessagingApi(kro, userIds);
    }
}
