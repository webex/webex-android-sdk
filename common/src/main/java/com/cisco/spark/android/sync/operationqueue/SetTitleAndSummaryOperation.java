package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.CONVERSATION_TITLE_AND_SUMMARY;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for marking a conversation read
 */
public class SetTitleAndSummaryOperation extends ActivityOperation {

    private String title;
    private String summary;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient Gson gson;

    public SetTitleAndSummaryOperation(Injector injector, String conversationId, String title, String summary) {
        super(injector, conversationId);
        this.title = title;
        this.summary = summary;
    }

    protected void configureActivity() {
        super.configureActivity(Verb.update);

        Conversation conversation = new Conversation(conversationId);
        conversation.setDisplayName(title);
        conversation.setSummary(summary);
        activity.setSource(ConversationContract.ActivityEntry.Source.LOCAL);
        activity.setObject(conversation);
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        Activity outgoingActivity;

        try {
            outgoingActivity = conversationProcessor.copyAndEncryptActivity(activity, keyUri);
        } catch (IOException e) {
            Ln.e(e, "Failed encrypting SetTitleAndSummary Activity");
            return SyncState.READY;
        }

        Response<Activity> response = postActivity(outgoingActivity);
        if (response.isSuccessful())
            return SyncState.SUCCEEDED;

        return SyncState.READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != CONVERSATION_TITLE_AND_SUMMARY)
            return false;

        if (((SetTitleAndSummaryOperation) newOperation).getConversationId().equals(getConversationId())) {
            // Setting title for the conversation again, the newer one wins.
            cancel();
        }
        return super.isOperationRedundant(newOperation);
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return CONVERSATION_TITLE_AND_SUMMARY;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }
}
