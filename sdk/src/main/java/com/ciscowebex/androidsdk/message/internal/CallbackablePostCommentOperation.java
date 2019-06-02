package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.conversation.Comment;
import com.cisco.spark.android.sync.operationqueue.ActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostCommentOperation;
import com.cisco.spark.android.sync.operationqueue.core.SyncState;
import com.cisco.spark.android.util.Action;

public class CallbackablePostCommentOperation extends PostCommentOperation {

    private Action<ActivityOperation> onOperationFinishAction;

    public CallbackablePostCommentOperation(Injector injector,
                                            String conversationId,
                                            Comment comment,
                                            Action<ActivityOperation> onOperationFinishAction) {
        super(injector, conversationId, comment);
        this.onOperationFinishAction = onOperationFinishAction;
    }

    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);
        if (this.onOperationFinishAction != null && (this.getState() == SyncState.FAULTED || this.getState() == SyncState.SUCCEEDED)) {
            this.onOperationFinishAction.call(this);
        }
    }
}
