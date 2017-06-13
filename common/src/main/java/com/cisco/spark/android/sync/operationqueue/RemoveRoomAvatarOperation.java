package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Content;
import com.cisco.spark.android.model.ContentCategory;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.Operation;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.ASSIGN_ROOM_AVATAR;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.REMOVE_ROOM_AVATAR;

public class RemoveRoomAvatarOperation extends ActivityOperation {

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    public RemoveRoomAvatarOperation(Injector injector, String conversationId) {
        super(injector, conversationId);
    }

    protected void configureActivity() {
        super.configureActivity(Verb.unassign);

        activity.setSource(ConversationContract.ActivityEntry.Source.LOCAL);
        activity.setObject(new Content(ContentCategory.IMAGES));
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        super.doWork();

        Activity outgoingActivity = conversationProcessor.copyAndEncryptActivity(activity, keyUri);
        Response<Activity> response = postActivity(outgoingActivity);
        if (response.isSuccessful())
            return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;

        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);

        if (newOperation.getOperationType() != REMOVE_ROOM_AVATAR && newOperation.getOperationType() != ASSIGN_ROOM_AVATAR)
            return;

        // If assigning or removing the avatar again, the newer operation wins.
        if (((ActivityOperation) newOperation).getConversationId().equals(getConversationId())) {
            cancel();
        }
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return REMOVE_ROOM_AVATAR;
    }
}
