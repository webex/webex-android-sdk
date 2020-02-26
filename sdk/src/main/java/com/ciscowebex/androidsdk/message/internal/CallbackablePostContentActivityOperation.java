package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.conversation.Comment;
import com.cisco.spark.android.model.conversation.File;
import com.cisco.spark.android.model.conversation.ParentObject;
import com.cisco.spark.android.sync.operationqueue.ActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation;
import com.cisco.spark.android.sync.operationqueue.core.SyncState;
import com.cisco.spark.android.util.Action;

import java.util.List;

public class CallbackablePostContentActivityOperation extends PostContentActivityOperation {

    private Action<ActivityOperation> onOperationFinishAction;

    private ParentObject parent;

    CallbackablePostContentActivityOperation(Injector injector,
                                             String conversationId,
                                             ShareContentData shareContentData,
                                             Comment comment,
                                             List<File> content,
                                             List<String> operationIds,
                                             ParentObject parent,
                                             Action<ActivityOperation> onOperationFinishAction) {
        super(injector, conversationId, shareContentData, comment, content, operationIds);
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
