package com.cisco.spark.android.sync.operationqueue;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Team;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.SET_TEAM_COLOR;

/**
 * Operation for setting the color for a team
 */
public class UpdateTeamColorOperation extends ActivityOperation {
    private String hexColorString;
    private String primaryConversationId;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient Gson gson;

    public UpdateTeamColorOperation(Injector injector, String teamId, String hexColorString, String primaryConversationId) {
        super(injector, teamId);
        this.hexColorString = hexColorString;
        this.primaryConversationId = primaryConversationId;
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.update);

        Team team = new Team(conversationId);
        team.setTeamColor(hexColorString);
        team.setGeneralConversationUuid(primaryConversationId);
        activity.setObject(team);
        activity.setTarget(team);
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        super.doWork();

        Uri keyUri = ConversationContentProviderQueries.getTitleEncryptionKeyUrlForConversation(getContentResolver(), primaryConversationId);

        if (keyUri == null) {
            Ln.e("Failed to set team color because we don't have a key URI");
            return ConversationContract.SyncOperationEntry.SyncState.FAULTED;
        }

        activity.setEncryptionKeyUrl(keyUri);
        ((Team) activity.getObject()).setEncryptionKeyUrl(keyUri);
        ((Team) activity.getTarget()).setEncryptionKeyUrl(keyUri);

        Response<Activity> response = postActivity(activity);

        if (response.isSuccessful())
            return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;

        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return SET_TEAM_COLOR;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);

        if (newOperation.getOperationType() != SET_TEAM_COLOR)
            return;

        if (((UpdateTeamColorOperation) newOperation).getConversationId().equals(getConversationId())) {
            // Setting color for the team again, the newer one wins
            cancel();
        }
    }
}
