package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.JOIN_TEAM_ROOM;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class JoinTeamRoomOperation extends Operation implements ConversationOperation {
    private String conversationId;
    private String parentTeamId;
    private int participantCount;
    private Conversation result;

    @Inject
    transient ApiClientProvider apiClientProvider;
    @Inject
    transient Provider<Batch> batchProvider;
    @Inject
    transient EventBus bus;
    @Inject
    transient Gson gson;
    @Inject
    transient ApiTokenProvider apiTokenProvider;

    public JoinTeamRoomOperation(Injector injector, String conversationId, String parentTeamId, int participantCount) {
        super(injector);
        this.conversationId = conversationId;
        this.parentTeamId = parentTeamId;
        this.participantCount = participantCount;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(30, TimeUnit.SECONDS)
                .withRetryDelay(0)
                .withMaxAttempts(3);
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return JOIN_TEAM_ROOM;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        Response<Conversation> response = apiClientProvider.getConversationClient().joinTeamConversation(parentTeamId, conversationId).execute();
        if (response.isSuccessful() && response.body() != null) {
            // Set joined status right away to allow the UI to properly update
            setConversationJoined(conversationId);
            Ln.i("Successfully joined team room " + getConversationId());
            return SyncState.SUCCEEDED;
        }

        Ln.e("Failed to join team room: %s", LoggingUtils.toString(response));
        return SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        //isOperationRedundant handles dupes, so we don't have to worry about this
    }

    @Override
    protected ConversationContract.SyncOperationEntry.SyncState checkProgress() {
        return getState();
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == JOIN_TEAM_ROOM && TextUtils.equals(conversationId, ((ConversationOperation) newOperation).getConversationId());
    }

    @Override
    protected void onStateChanged(SyncState oldState) {

        if (getState() == SyncState.FAULTED) {
            Ln.i("Join team room operation faulted, posting event to notify ConversationFragment");
            bus.post(new JoinTeamRoomOperationFailedEvent(this));
        } else {
            super.onStateChanged(oldState);
        }
    }

    public String getConversationId() {
        return conversationId;
    }

    private void setConversationJoined(final String conversationId) {
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                Batch batch = batchProvider.get();
                batch.add(ConversationContentProviderOperation.setConversationJoined(conversationId));
                batch.apply();
                return null;
            }
        }.execute();
    }

    public static class JoinTeamRoomOperationFailedEvent {
        public JoinTeamRoomOperation operation;

        JoinTeamRoomOperationFailedEvent(JoinTeamRoomOperation op) {
            this.operation = op;
        }
    }
}
