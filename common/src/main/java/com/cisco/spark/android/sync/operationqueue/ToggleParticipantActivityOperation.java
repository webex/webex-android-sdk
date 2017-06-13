package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.OperationCompletedEvent;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.ParticipantEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;

public class ToggleParticipantActivityOperation extends ActivityOperation {

    @Inject
    transient protected ActorRecordProvider actorRecordProvider;

    @Inject
    transient protected EventBus bus;

    private ActorRecord.ActorKey actorKey;
    private String verb;
    private OperationType operationType;
    private int serverStatusCode;

    public ToggleParticipantActivityOperation(Injector injector, String conversationId, ActorRecord.ActorKey actorKey, String verb, OperationType operationType) {
        super(injector, conversationId);
        this.actorKey = actorKey;
        this.verb = verb;
        this.operationType = operationType;
    }

    @Override
    protected void configureActivity() {
        configureActivity(verb);
        ActorRecord actor = actorRecordProvider.get(actorKey);
        activity.setObject(new Person(actor));
    }

    @NonNull
    @Override
    protected SyncOperationEntry.SyncState doWork() throws IOException {
        super.doWork();

        Response<Activity> response = postActivity(activity);

        if (response.isSuccessful()) {
            return SyncOperationEntry.SyncState.SUCCEEDED;
        }

        if (response.code() == 400 || (response.code() == 403 && getOperationType() == OperationType.ASSIGN_MODERATOR)) {
            try {
                ErrorDetail errorDetail = gson.fromJson(response.errorBody().string(), ErrorDetail.class);
                serverStatusCode = errorDetail.getErrorCode();
                return SyncOperationEntry.SyncState.FAULTED;
            } catch (Exception ex) {
                Ln.e(ex, "Error decoding error response");
            }
        }

        return SyncOperationEntry.SyncState.READY;
    }

    @Override
    protected void onStateChanged(SyncOperationEntry.SyncState oldState) {
        super.onStateChanged(oldState);
        if (getState() == SyncOperationEntry.SyncState.FAULTED) {
            bus.post(new OperationCompletedEvent(this));

            new SafeAsyncTask<Void>() {
                @Override
                public Void call() throws Exception {
                    Batch batch = newBatch();

                    String where = ParticipantEntry.CONVERSATION_ID.name() + " = ? AND " + ParticipantEntry.ACTOR_UUID.name() + " = ?";

                    ContentProviderOperation operation = ContentProviderOperation
                            .newUpdate(ParticipantEntry.CONTENT_URI)
                            .withValue(ParticipantEntry.IS_MODERATOR.name(), false)
                            .withSelection(where, new String[]{getConversationId(), getParticipantId().getUuid()}).build();

                    batch.add(operation);
                    batch.apply();
                    return null;
                }

            }.execute();
        }
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);

        if (newOperation.getOperationType() != operationType)
            return;

        if (((ToggleParticipantActivityOperation) newOperation).getConversationId().equals(getConversationId()) && ((ToggleParticipantActivityOperation) newOperation).getParticipantId().equals(getParticipantId())) {
            // Toggling the same conversation again, the newer one wins.
            cancel();
        }
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return operationType;
    }

    public ActorRecord.ActorKey getParticipantId() {
        return actorKey;
    }

    public int getServerStatusCode() {
        return serverStatusCode;
    }
}
