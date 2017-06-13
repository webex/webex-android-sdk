package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

public class RemoveParticipantOperation extends ActivityOperation {

    @Inject
    transient KeyManager keyManager;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient EventBus bus;

    private ActorRecord.ActorKey actorKey;
    private KmsResourceObject kro;

    public ActorRecord.ActorKey getActorKey() {
        return actorKey;
    }

    // Constructor for self-leave
    public RemoveParticipantOperation(Injector injector, String conversationId) {
        super(injector, conversationId);
        this.actorKey = apiTokenProvider.getAuthenticatedUser().getKey();
    }

    public RemoveParticipantOperation(Injector injector, String conversationId, ActorRecord.ActorKey actorKey) {
        super(injector, conversationId);
        this.actorKey = actorKey;
    }

    @Override
    protected void configureActivity() {
        configureActivity(Verb.leave);

        kro = ConversationContentProviderQueries.getKmsResourceObject(
                getContentResolver(), conversationId);

        // Additional guard added because of
        // BEMS464148-WIP Verizon cannot remove user from room
        // The actorKey.getUuid is an email address for an as yet undetermined reason, causing the KMS message to get rejected.
        // The following block makes a best effort to get the real UUID. If we fail, skip the KMS message to de-authorize the user.
        // TODO there may be other places that need this logic
        actorKey = validateActorKey(actorKey);
        activity.setObject(new Person(actorKey));
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.REMOVE_PARTICIPANT;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        if (keyManager.getSharedKeyWithKMS() == null) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            return SyncState.READY;
        }

        if (!Strings.isEmailAddress(actorKey.getUuid()))
            activity.setEncryptedKmsMessage(conversationProcessor.removeParticipantUsingKmsMessagingApi(kro, actorKey.getUuid()));

        Response<Activity> response = apiClientProvider.getConversationClient().postActivity(activity).execute();
        if (response.isSuccessful()) {
            bus.post(new RemoveParticipantOperationSucceededEvent(this));
            return SyncState.SUCCEEDED;
        }

        Ln.w("Failed removing participant: " + LoggingUtils.toString(response));

        if (response.code() == 400) {
            String message = CryptoUtils.getKmsErrorMessage(response, gson, keyManager.getSharedKeyAsJWK());
            setErrorMessage(message);
        }

        bus.post(new RemoveParticipantOperationFailedEvent(this));
        return SyncState.FAULTED;
    }

    public static class RemoveParticipantOperationFailedEvent {
        public RemoveParticipantOperation operation;

        public RemoveParticipantOperationFailedEvent(RemoveParticipantOperation op) {
            this.operation = op;
        }
    }

    public static class RemoveParticipantOperationSucceededEvent {
        public RemoveParticipantOperation operation;

        public RemoveParticipantOperationSucceededEvent(RemoveParticipantOperation op) {
            this.operation = op;
        }
    }
}
