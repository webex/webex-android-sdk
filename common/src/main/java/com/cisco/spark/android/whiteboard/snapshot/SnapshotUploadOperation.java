package com.cisco.spark.android.whiteboard.snapshot;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.ContentUploadOperation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;

import java.io.IOException;

import de.greenrobot.event.EventBus;

public class SnapshotUploadOperation extends ContentUploadOperation {

    private transient final EventBus bus;
    private final Callback callback;

    public SnapshotUploadOperation(Injector injector, EventBus bus, String conversationId, File file, @Nullable Callback callback) {
        super(injector, conversationId, file);
        this.bus = bus;
        this.callback = callback;
    }

    public SnapshotUploadOperation(Injector injector, EventBus bus, Uri spaceUrl, File content, @Nullable Callback callback) {
        super(injector, spaceUrl, content);
        this.bus = bus;
        this.callback = callback;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        ConversationContract.SyncOperationEntry.SyncState syncState = super.doWork();

        File file = getFiles().get(0);
        if (syncState == ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED) {
            callback.onSuccess(getOperationId(), file);
        } else if (syncState == ConversationContract.SyncOperationEntry.SyncState.FAULTED) {
            callback.onFailure(getOperationId(), "SnapshotUploadOperation failed");
        }
        return syncState;

    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(1);
    }

    public interface Callback {
        void onSuccess(String operationId, File file);
        void onFailure(String operationId, String errorMessage);
    }
}
