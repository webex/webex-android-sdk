package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationRecord;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import static com.cisco.spark.android.sync.ConversationContract.EncryptionKeyEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import static com.cisco.spark.android.sync.ConversationContract.vw_Participant;

public class UpdateEncryptionKeyOperation extends ActivityOperation {

    @Inject
    protected transient EncryptedConversationProcessor encryptedConversationProcessor;

    @Inject
    protected transient ActorRecordProvider actorRecordProvider;

    @Inject
    protected transient KeyManager keyManager;

    @Inject
    protected transient DeviceRegistration deviceRegistration;

    // don't persist this
    protected transient KeyObject newKey;

    public UpdateEncryptionKeyOperation(Injector injector, String conversationId) {
        super(injector, conversationId);
        this.conversationId = conversationId;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.UPDATE_ENCRYPTION_KEY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOp) {
        if (newOp.getOperationType() != getOperationType())
            return false;

        if (getState().isTerminal())
            return false;

        if (TextUtils.equals(conversationId, ((UpdateEncryptionKeyOperation) newOp).conversationId))
            return true;

        return false;
    }

    @NonNull
    @Override
    public SyncState onPrepare() {
        SyncState ret = super.onPrepare();

        if (ret != SyncState.READY) {
            Ln.i(this + " Still PREPARING");
            return ret;
        }

        if (!keyManager.hasSharedKeyWithKMS()) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            ret = SyncState.PREPARING;
        }

        if (newKey == null) {
            // This will set newKey if possible, and update the outgoing activity
            setActivityObject();
        }

        if (newKey == null) {
            Ln.i(this + " Still waiting for a new key");
            ret = SyncState.PREPARING;
        }

        return ret;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        // If the conv already has a default key, take no further action
        if (remoteConversationHasDefaultKey()) {
            Ln.i("Skipping key update ; conversation already has a default key");
            return SyncState.SUCCEEDED;
        }

        setActivityObject();

        if (newKey == null)
            return SyncState.READY;

        ConversationRecord cr = getConversationRecord();

        if (cr == null) {
            ln.i("Failed to resolve convId " + conversationId);
            return SyncState.READY;
        }

        Set<String> participantUuids = getParticipantUuids();

        if (!keyManager.hasSharedKeyWithKMS()) {
            ln.i("No shared key with KMS");
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            getRetryPolicy().scheduleNow();
            return SyncState.PREPARING;
        }

        activity.setEncryptedKmsMessage(encryptedConversationProcessor
                .createNewResource(new ArrayList<>(participantUuids), Collections.singletonList(newKey.getKeyId())));

        Response<Activity> response = postActivity(activity);

        if (response.isSuccessful()) {
            Activity result = response.body();
            KmsResponseBody kmsResult = CryptoUtils.decryptKmsMessage(result.getEncryptedKmsMessage(), keyManager.getSharedKeyAsJWK());
            if (kmsResult != null && kmsResult.getResource() != null && kmsResult.getResource().getUri() != null) {
                cr.setKmsResourceObject(new KmsResourceObject(kmsResult.getResource().getUri()));
                writeNewKroToDb(cr.getKmsResourceObject());
                writeNewKeyToDb();
                return SyncState.SUCCEEDED;
            }
        } else if (response.code() == 400) {
            KmsResponseBody kmsResponseBody = CryptoUtils.extractKmsResponseBody(response, gson, keyManager.getSharedKeyAsJWK());
            if (kmsResponseBody != null) {
                String message = kmsResponseBody.getReason();
                Ln.w("Kms Response: " + message + " : " + kmsResponseBody.getStatus());
                setErrorMessage(message);
            }
        }

        return SyncState.READY;
    }

    private boolean remoteConversationHasDefaultKey() {
        Response<Conversation> response = null;
        try {
            response = apiClientProvider.getConversationClient().getConversation(conversationId).execute();
            if (response.isSuccessful()) {
                return response.body().getDefaultActivityEncryptionKeyUrl() != null;
            }
        } catch (IOException e) {
            Ln.e(e);
        }

        throw new RuntimeException("Failed getting conversation from service: " + LoggingUtils.toString(response));
    }

    private ConversationRecord getConversationRecord() throws IOException {
        ConversationRecord cr = ConversationRecord.buildFromContentResolver(getContentResolver(), gson, conversationId, null);

        if (cr == null) {
            conversationSyncQueue.getConversationFrontFillTask(conversationId).execute();
            cr = ConversationRecord.buildFromContentResolver(getContentResolver(), gson, conversationId, null);
        }

        if (cr == null) {
            Response<Conversation> response = apiClientProvider.getConversationClient().getConversation(conversationId).execute();
            if (response.isSuccessful()) {
                cr = ConversationRecord.buildFromConversation(gson, response.body(), apiTokenProvider.getAuthenticatedUser(), null);
            } else {
                Ln.w("Failed getting conversation from service: " + LoggingUtils.toString(response));
            }
        }
        return cr;
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.updateKey);
        setActivityObject();
    }

    private void setActivityObject() {
        if (newKey == null) {
            newKey = keyManager.getCachedUnboundKey();
            if (newKey == null) {
                setDependsOn(operationQueue.refreshUnboundKeys());
            }
        }

        Conversation conversationObject = new Conversation(conversationId);

        if (newKey != null)
            conversationObject.setDefaultActivityEncryptionKeyUrl(newKey.getKeyUrl());

        activity.setObject(conversationObject);
    }

    /**
     * Normally the new key will be written already but this covers the corner cases, for example if
     * there are no keys available when the operation is enqueued.
     */
    private void writeNewKeyToDb() {
        if (newKey == null || newKey.getKey() == null || newKey.getKeyUrl() == null)
            return;

        Batch batch = newBatch();

        ContentProviderOperation cpo = ContentProviderOperation.newInsert(EncryptionKeyEntry.CONTENT_URI)
                .withValue(EncryptionKeyEntry.ENCRYPTION_KEY.name(), newKey.getKey())
                .withValue(EncryptionKeyEntry.ENCRYPTION_KEY_ID.name(), UriUtils.toString(newKey.getKeyId()))
                .withValue(EncryptionKeyEntry.ENCRYPTION_KEY_URI.name(), UriUtils.toString(newKey.getKeyUrl()))
                .build();

        batch.add(cpo);

        cpo = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withValue(ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL.name(), newKey.getKeyUrl().toString())
                .withSelection(ConversationEntry.CONVERSATION_ID + "=?", new String[]{conversationId})
                .build();

        batch.add(cpo);
        batch.apply();
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);

        if (newKey == null && newOperation.getOperationType() == OperationType.FETCH_UNBOUND_KEYS) {
            setDependsOn(newOperation);
        }
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    public Set<String> getParticipantUuids() {
        HashSet<String> ret = new HashSet<>();
        Cursor c = null;
        try {
            c = getContentResolver().query(vw_Participant.CONTENT_URI,
                    new String[]{vw_Participant.ACTOR_UUID.name()},
                    vw_Participant.CONVERSATION_ID + "=?",
                    new String[]{conversationId},
                    null);

            while (c != null && c.moveToNext()) {
                ret.add(c.getString(0));
            }
        } finally {
            if (c != null)
                c.close();
        }
        ret.remove(apiTokenProvider.getAuthenticatedUser().getKey().getUuid());
        return ret;
    }

    public void writeNewKroToDb(KmsResourceObject kro) {
        if (kro == null)
            return;

        Batch batch = newBatch();
        batch.add(ContentProviderOperation.newUpdate(Uri.withAppendedPath(ConversationEntry.CONTENT_URI, getConversationId()))
                .withValue(ConversationEntry.KMS_RESOURCE_OBJECT_URI.name(), kro.getUri().toString())
                .build()
        );
        batch.apply();
    }
}
