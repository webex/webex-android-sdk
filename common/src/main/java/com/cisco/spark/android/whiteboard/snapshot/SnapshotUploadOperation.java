package com.cisco.spark.android.whiteboard.snapshot;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.ContentUploadOperation;
import com.cisco.spark.android.whiteboard.SnapshotUploadOperationCompleteEvent;

import java.io.IOException;

import de.greenrobot.event.EventBus;

public class SnapshotUploadOperation extends ContentUploadOperation {

    private transient final EventBus bus;

    public SnapshotUploadOperation(Injector injector, EventBus bus, String conversationId, File content) {
        super(injector, conversationId, content);
        this.bus = bus;
    }

    public SnapshotUploadOperation(Injector injector, EventBus bus, Uri spaceUrl, File content) {
        super(injector, spaceUrl, content);
        this.bus = bus;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        ConversationContract.SyncOperationEntry.SyncState syncState = super.doWork();

        if (syncState == ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED) {
            bus.post(new SnapshotUploadOperationCompleteEvent(getOperationId(), true));
        } else if (syncState == ConversationContract.SyncOperationEntry.SyncState.FAULTED) {
            bus.post(new SnapshotUploadOperationCompleteEvent(getOperationId(), false));
        }
        return syncState;
    }
}
