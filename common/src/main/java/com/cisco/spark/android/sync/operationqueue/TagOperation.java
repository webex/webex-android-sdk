package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.ConversationTag;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.github.benoitdion.ln.Ln;

import java.util.List;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for setting 'favorite' or 'mute' state on a conversation, or any other toggles that come along
 */
public class TagOperation extends ActivityOperation {

    protected List<ConversationTag> conversationTagList;
    private boolean tag;
    private OperationType operationType;

    protected boolean isTag() {
        return tag;
    }

    public TagOperation(Injector injector, String conversationId, List<ConversationTag> conversationTagList, boolean tag, OperationType operationType) {
        super(injector, conversationId);
        this.conversationTagList = conversationTagList;
        this.tag = tag;
        this.operationType = operationType;
    }

    protected void configureActivity() {
        if (tag) {
            super.configureActivity(Verb.tag);
        } else {
            super.configureActivity(Verb.untag);
        }
        Conversation conversation = new Conversation(conversationId);
        conversation.getTags().addAll(conversationTagList);
        activity.setObject(conversation);
    }

    @NonNull
    @Override
    protected SyncState doWork() {
        try {
            super.doWork();
            Response<Activity> response = postActivity(activity);
            if (response.isSuccessful()) {
                return SyncState.SUCCEEDED;
            }
        } catch (Exception e) {
            Ln.w(e, "Failed to tag conversation");
        }
        return SyncState.READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() == operationType) {
            if (((TagOperation) newOperation).getConversationId().equals(getConversationId())) {
                // Toggling the same conversation again, the newer one wins.
                cancel();
            }
        }
        return false;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return operationType;
    }
}
