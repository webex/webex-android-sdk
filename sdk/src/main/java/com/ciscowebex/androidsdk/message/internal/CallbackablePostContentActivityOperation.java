package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.conversation.Comment;
import com.cisco.spark.android.model.conversation.File;
import com.cisco.spark.android.sync.operationqueue.ActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation;
import com.cisco.spark.android.sync.operationqueue.core.SyncState;
import com.cisco.spark.android.util.Action;

import java.util.List;

public class CallbackablePostContentActivityOperation extends PostContentActivityOperation {

    private Action<ActivityOperation> onOperationFinishAction;

    CallbackablePostContentActivityOperation(Injector injector,
                                             String conversationId,
                                             ShareContentData shareContentData,
                                             Comment comment,
                                             List<File> content,
                                             List<String> operationIds,
                                             Action<ActivityOperation> onOperationFinishAction) {
        super(injector, conversationId, shareContentData, comment, content, operationIds);
        this.onOperationFinishAction = onOperationFinishAction;
    }

    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);
        if (this.onOperationFinishAction != null && (this.getState() == SyncState.FAULTED || this.getState() == SyncState.SUCCEEDED)) {
            this.onOperationFinishAction.call(this);
        }
    }

}
