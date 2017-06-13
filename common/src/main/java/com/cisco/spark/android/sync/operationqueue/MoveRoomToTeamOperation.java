package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.ObjectType;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

public class MoveRoomToTeamOperation extends ActivityOperation {
    private String teamId;
    private boolean moveIntoTeam;
    private Uri teamKROURI;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    public MoveRoomToTeamOperation(Injector injector, String conversationId, String teamId, boolean moveIntoTeam) {
        super(injector, conversationId);
        this.teamId = teamId;
        this.moveIntoTeam = moveIntoTeam;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(30, TimeUnit.SECONDS)
                .withMaxAttempts(3)
                .withRetryDelay(0);
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(moveIntoTeam ? Verb.add : Verb.remove);
        activity.getTarget().setObjectType(ObjectType.team);
        activity.getTarget().setId(teamId);
        activity.setObject(new Conversation(conversationId));
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.MOVE_ROOM_TO_TEAM;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return super.onEnqueue();
    }

    @NonNull
    @Override
    public SyncState onPrepare() {
        SyncState ret = super.onPrepare();
        if (ret == SyncState.READY) {
            if (teamKROURI == null && teamId != null) {
                teamKROURI = KeyManager.getTeamKroUri(getContentResolver(), apiClientProvider, teamId);
            }
        }
        return teamKROURI != null
                ? SyncState.READY
                : SyncState.PREPARING;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();
        KmsResourceObject kro = ConversationContentProviderQueries.getKmsResourceObject(
                getContentResolver(), conversationId);

        if (teamKROURI == null) {
            Ln.i("Trying to add a room to a team but missing the team's KRO URI, bailing out and waiting for a retry");
            return SyncState.READY;
        }

        if (!keyManager.hasSharedKeyWithKMS()) {
            Operation setupSharedKeyOperation = operationQueue.setUpSharedKey();
            setDependsOn(setupSharedKeyOperation);
            return SyncState.READY;
        }

        activity.setEncryptedKmsMessage(conversationProcessor.authorizeNewParticipantsUsingKmsMessagingApi(kro, Collections.singleton(teamKROURI.toString())));

        Response<Activity> response = postActivity(activity);


        Batch batch = newBatch();
        ContentProviderOperation op = ContentProviderOperation
                .newUpdate(ConversationContract.ConversationEntry.CONTENT_URI)
                .withSelection(ConversationContract.ConversationEntry.CONVERSATION_ID + "=?", new String[]{conversationId})
                .withValue(ConversationContract.ConversationEntry.TEAM_ID.name(), moveIntoTeam && response.isSuccessful() ? teamId : null)
                .build();

        batch.add(op);
        batch.apply();

        return response.isSuccessful() ? SyncState.SUCCEEDED : SyncState.READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != ConversationContract.SyncOperationEntry.OperationType.MOVE_ROOM_TO_TEAM)
            return false;

        MoveRoomToTeamOperation newOp = (MoveRoomToTeamOperation) newOperation;

        return TextUtils.equals(conversationId, newOp.getConversationId())
                && teamId.equals(newOp.getTeamId());
    }

    public String getTeamId() {
        return teamId;
    }
}
