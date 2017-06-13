package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ObjectType;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;

import java.io.IOException;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;

public class DeleteActivityOperation extends ActivityOperation {

    private String activityId;

    public DeleteActivityOperation(Injector injector, String conversationId, String activityId) {
        super(injector, conversationId);
        this.activityId = activityId;
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.delete);

        Activity object = new Activity();
        object.setId(activityId);
        object.setObjectType(ObjectType.activity);

        activity.setObject(object);
    }

    @NonNull
    @Override
    public SyncOperationEntry.OperationType getOperationType() {
        return SyncOperationEntry.OperationType.DELETE_ACTIVITY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != SyncOperationEntry.OperationType.DELETE_ACTIVITY)
            return false;

        DeleteActivityOperation op = (DeleteActivityOperation) newOperation;
        if (op.getConversationId().equals(getConversationId()) && op.activityId.equals(this.activityId)) {
            return true;
        }

        return false;
    }

    // Deletes shouldn't give up unless they run out of attempts. The delete is represented locally
    // and the user will expect it to be gone even if they've been offline for a while.
    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        // TODO it's possible the activityid is provisional. In that case the delete will fail and the
        // user will have to delete again. We can't resolve its real activityId because the activity row
        // was deleted in onEnqueue. To fix we need to soft-delete the activity until the delete syncs
        // back from the server.

        Response<Activity> response = postActivity(activity);
        if (response.isSuccessful())
            return SyncState.SUCCEEDED;

        return SyncState.READY;
    }

    public String getActivityToDelete() {
        return activityId;
    }
}
