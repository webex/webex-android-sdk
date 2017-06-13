package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.mercury.events.KmsMessageResponseEvent;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.KmsMessageRequestType;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for fetching unbound keys from the KMS
 */
public class CreateKmsResourceOperation extends PostKmsMessageOperation implements ConversationOperation {

    private final Collection<String> userIds;
    private String conversationId;
    private KmsResourceObject kro;

    //don't persist these
    private transient KeyObject newKey;
    private transient String requestId;

    @Inject
    transient AuthenticatedUserProvider authenticatedUserProvider;

    @Inject
    transient EventBus bus;


    public CreateKmsResourceOperation(Injector injector, String conversationId, Collection<String> userIds) {
        super(injector);
        this.conversationId = conversationId;
        this.userIds = userIds;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.KMS_CREATE_RESOURCE;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    public SyncState onPrepare() {
        if (newKey == null)
            newKey = keyManager.getCachedUnboundKey();

        if (newKey == null)
            return SyncState.PREPARING;

        return super.onPrepare();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        Response<Conversation> response = null;
        response = apiClientProvider.getConversationClient().getConversation(conversationId).execute();
        if (!response.isSuccessful()) {
            Ln.w("Failed getting conversation: " + LoggingUtils.toString(response));
            return SyncState.READY;
        }

        Conversation conv = response.body();
        if (conv.getKmsResourceObject() != null
                && conv.getKmsResourceObject().getUri().toString().startsWith("http")
                && conv.getDefaultActivityEncryptionKeyUrl() != null) {
            ln.i("Resource already exists for " + conversationId + ". Skipping operation");
            writeKroToDb(conv.getKmsResourceObject().getURI());
            this.kro = conv.getKmsResourceObject();
            return SyncState.SUCCEEDED;
        }

        if (!bus.isRegistered(this))
            bus.register(this);

        requestId = UUID.randomUUID().toString();

        KmsRequest request = CryptoUtils.createResourceRequest(deviceRegistration.getId(), authenticatedUserProvider.getAuthenticatedUser(), requestId, new ArrayList<>(userIds), newKey.getKeyId());
        sendKmsMessage(request.asEncryptedBlob(keyManager.getSharedKeyAsJWK()), requestId, KmsMessageRequestType.CREATE_RESOURCE, request);

        // We're EXECUTING until the kms event comes back with success
        return SyncState.EXECUTING;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {

    }

    private void writeKroToDb(URI kroUri) {
        if (kroUri == null)
            return;

        Batch batch = newBatch();
        batch.add(ContentProviderOperation.newUpdate(Uri.withAppendedPath(ConversationContract.ConversationEntry.CONTENT_URI, getConversationId()))
                        .withValue(ConversationContract.ConversationEntry.KMS_RESOURCE_OBJECT_URI.name(), kroUri.toString())
                        .build()
        );
        batch.apply();
    }

    /**
     * When the KMS responds with success the onEvent handler sets the kro field. Here's where we
     * see if that field has been set yet.
     *
     * @return SUCCEEDED if the kro has been populated. Otherwise no change.
     */
    @Override
    public SyncState checkProgress() {
        if (kro != null)
            return SyncState.SUCCEEDED;

        return getState();
    }

    @Override
    public String toString() {
        return super.toString() + " (" + conversationId + ")";
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == this.getOperationType()
                && !getState().isTerminal()
                && TextUtils.equals(getConversationId(), ((ConversationOperation) newOperation).getConversationId());
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    // Listen for the response from the KMS and set the kro.
    public void onEvent(KmsMessageResponseEvent event) throws ParseException {
        List<KmsResponseBody> decryptedKmsMessages = CryptoUtils.decryptKmsMessages(event.getEncryptionKmsMessage(), keyManager.getSharedKeyAsJWK());
        for (KmsResponseBody decryptedKmsMessage : decryptedKmsMessages) {
            if (TextUtils.equals(decryptedKmsMessage.getRequestId(), requestId)) {
                if (decryptedKmsMessage.getResource() != null) {
                    writeKroToDb(decryptedKmsMessage.getResource().getUri());
                    this.kro = new KmsResourceObject(decryptedKmsMessage.getResource().getUri());
                } else {
                    Ln.w("Unexpected response getting shared key: " + decryptedKmsMessage.getStatus() + " " + decryptedKmsMessage.getReason());
                }
            }
        }
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);
        if (getState().isTerminal() && bus.isRegistered(this))
            bus.unregister(this);
    }
}
