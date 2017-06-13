package com.cisco.spark.android.sync.operationqueue.core;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.ConversationOperation;
import com.cisco.spark.android.sync.queue.ConversationFrontFillTask;

import java.io.IOException;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState.READY;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;

public class FetchParticipantsOperation extends Operation implements ConversationOperation {
    private final String conversationId;

    public FetchParticipantsOperation(Injector injector, String conversationId) {
        super(injector);
        this.conversationId = conversationId;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.FETCH_PARTICIPANTS;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        ConversationFrontFillTask task = new ConversationFrontFillTask(injector, conversationId);
        task.execute();

        String isComplete = ConversationContentProviderQueries.getOneValue(getContentResolver(),
                ConversationContract.ConversationEntry.IS_PARTICIPANT_LIST_VALID,
                ConversationContract.ConversationEntry.CONVERSATION_ID + "=?",
                new String[]{conversationId});

        return ("1".equals(isComplete))
                ? SUCCEEDED
                : READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != getOperationType())
            return false;

        FetchParticipantsOperation newFetchParticipantsOp = (FetchParticipantsOperation) newOperation;

        return TextUtils.equals(newFetchParticipantsOp.getConversationId(), getConversationId());
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }
}
