package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.EventObject;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation that represents an operation on an EventObject (e.g. Scheduled SyncUps)
 */
public class ScheduledEventActivityOperation extends ActivityOperation {
    private String verb;
    private EventObject eventObject;

    @Inject
    transient protected EncryptedConversationProcessor conversationProcessor;

    public ScheduledEventActivityOperation(Injector injector, String conversationId, String verb, EventObject eventObject) {
        super(injector, conversationId);
        this.verb = verb;
        this.eventObject = eventObject;
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(verb);
        activity.setObject(eventObject);
        addLocationData();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();

        if (keyUri == null)
            return SyncState.READY;

        try {
            postActivity(conversationProcessor.copyAndEncryptActivity(activity, keyUri));
            return SyncState.SUCCEEDED;
        } catch (IOException e) {
            Ln.e(e);
            return SyncState.READY;
        }
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.SCHEDULED_EVENT;
    }
}
