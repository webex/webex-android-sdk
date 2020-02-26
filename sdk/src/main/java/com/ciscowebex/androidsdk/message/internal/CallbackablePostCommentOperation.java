package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.conversation.Comment;
import com.cisco.spark.android.model.conversation.ParentObject;
import com.cisco.spark.android.sync.operationqueue.ActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostCommentOperation;
import com.cisco.spark.android.sync.operationqueue.core.SyncState;
import com.cisco.spark.android.util.Action;

public class CallbackablePostCommentOperation extends PostCommentOperation {

    private Action<ActivityOperation> onOperationFinishAction;
    private ParentObject parent;

    CallbackablePostCommentOperation(Injector injector,
                                     String conversationId,
                                     Comment comment,
                                     ParentObject parent,
                                     Action<ActivityOperation> onOperationFinishAction) {
        super(injector, conversationId, comment);
        this.onOperationFinishAction = onOperationFinishAction;
        this.parent = parent;
    }

    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);
        if (this.onOperationFinishAction != null && (this.getState() == SyncState.FAULTED || this.getState() == SyncState.SUCCEEDED)) {
            this.onOperationFinishAction.call(this);
        }
    }

    @Override
    protected void configureActivity() {
        super.configureActivity();
        activity.setParent(parent);
    }
}
