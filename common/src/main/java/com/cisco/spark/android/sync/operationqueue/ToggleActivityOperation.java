package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.sync.operationqueue.core.Operation;

import java.io.IOException;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for setting 'favorite' or 'mute' state on a conversation, or any other toggles that
 * come along
 */
public class ToggleActivityOperation extends ActivityOperation {

    private String verb;
    private OperationType operationType;

    public ToggleActivityOperation(Injector injector, String conversationId, String verb, OperationType operationType) {
        super(injector, conversationId);
        this.verb = verb;
        this.operationType = operationType;
    }

    protected void configureActivity() {
        super.configureActivity(verb);
        activity.setObject(new Conversation(conversationId));
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();
        Response<Activity> response = postActivity(activity);

        if (response.isSuccessful())
            return SyncState.SUCCEEDED;

        return SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);

        if (newOperation.getOperationType() != operationType)
            return;

        if (((ToggleActivityOperation) newOperation).getConversationId().equals(getConversationId())) {
            // Toggling the same conversation again, the newer one wins.
            cancel();
        }
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return operationType;
    }
}
