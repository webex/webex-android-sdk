package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import com.cisco.spark.android.client.CalendarServiceClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.EventToRoomMapping;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationRecord;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

public class MapEventToConversationOperation extends Operation implements ConversationOperation {
    private Date endDate;
    private String eventId;
    private String provisionalConversationId;
    private boolean cacheOnly;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient Gson gson;

    public MapEventToConversationOperation(Injector injector, String eventId, Date endDate, String conversationId, boolean cacheOnly) {
        super(injector);
        this.eventId = eventId;
        this.endDate = endDate;
        this.provisionalConversationId = conversationId;
        this.cacheOnly = cacheOnly;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.MAP_EVENT_TO_CONVERSATION;
    }

    /**
     * When this Operation is enqueued, a matching NewConversationOperation will set this Operation to depend on it, so doWork
     * will not get called until after NewConversationOperation has completed.
     */
    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        // Store the mapping in the database before attempting to persist in the cloud.
        ContentValues values = new ContentValues();
        values.put(ConversationContract.ConversationMeetingEntry.CONVERSATION_ID.name(), provisionalConversationId);
        values.put(ConversationContract.ConversationMeetingEntry.MEETING_ID.name(), eventId);
        getContentResolver().insert(ConversationContract.ConversationMeetingEntry.CONTENT_URI, values);

        if (cacheOnly) {
            return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;
        } else {
            return ConversationContract.SyncOperationEntry.SyncState.READY;
        }
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() {
        // The conversation has now been created, so use its real id.
        ConversationRecord conversationRecord = ConversationRecord.buildFromContentResolver(getContentResolver(), gson, provisionalConversationId, null);
        String conversationId = conversationRecord != null ? conversationRecord.getId() : provisionalConversationId;

        // Persist this mapping via the API
        CalendarServiceClient calendarServiceClient = apiClientProvider.getCalendarServiceClient();
        Response response = null;
        try {
            response = calendarServiceClient.mapEventToRoom(
                    new EventToRoomMapping(eventId, UUID.fromString(conversationId),
                            new Date(endDate.getTime() + TimeUnit.DAYS.toMillis(1)))).execute();
        } catch (IOException e) {
            Ln.e(e);
        }

        if (response != null && response.isSuccessful()) {
            return ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;
        } else {
            return ConversationContract.SyncOperationEntry.SyncState.READY;
        }
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    protected ConversationContract.SyncOperationEntry.SyncState checkProgress() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }

    @Override
    public boolean needsNetwork() {
        return !cacheOnly;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withExponentialBackoff();
    }

    public String getConversationId() {
        return provisionalConversationId;
    }
}
