package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.SegmentService;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.github.benoitdion.ln.Ln;
import com.segment.analytics.Properties;

import java.io.IOException;
import java.util.Date;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.MARK_CONVERSATION_READ;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for marking a conversation read
 */
public class MarkReadOperation extends ActivityOperation {
    private static final long NOW = -1; // If no lastSeenActivityTimestamp is passed to this class, use NOW

    private long lastSelfAckTimestamp;
    private long lastSeenActivityTimestamp;


    public MarkReadOperation(Injector injector, String conversationId) {
        super(injector, conversationId);
        lastSeenActivityTimestamp = NOW;
    }

    public MarkReadOperation(Injector injector, String conversationId, long lastSeenActivityTimestamp) {
        super(injector, conversationId);
        this.lastSeenActivityTimestamp = lastSeenActivityTimestamp;
    }

    protected void configureActivity() {
        super.configureActivity(Verb.acknowledge);

        ActivityReference activityReference;
        if (lastSeenActivityTimestamp == NOW) {
            activityReference = ConversationContentProviderQueries.getLastAckableActivity(getContentResolver(), conversationId);
        } else {
            lastSelfAckTimestamp = ConversationContentProviderQueries.getLastSelfAckTimestamp(getContentResolver(), getConversationId());
            if (lastSelfAckTimestamp > lastSeenActivityTimestamp) {
                Ln.d("Not sending and ack for conversation activity at time " + String.valueOf(lastSeenActivityTimestamp) + " because we've already sent an ack for a more recent activity in this conversation");
                cancel();
                return;
            }

            activityReference = ConversationContentProviderQueries.getLastAckableActivityBeforeTime(getContentResolver(), conversationId, lastSeenActivityTimestamp + 1);
        }

        if (activityReference == null) {
            Ln.i("Failed marking conversation read because it has no activities");
            cancel();
            return;
        }

        Activity object = new Activity();
        object.setId(activityReference.getActivityId());
        object.setPublished(new Date(activityReference.getPublishTime()));
        object.setActivityType(activityReference.getType());
        activity.setObject(object);
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();
        Response<Activity> response = postActivity(activity);
        if (response.isSuccessful()) {
            postSegmentMetrics(response);
            return SyncState.SUCCEEDED;
        }

        return SyncState.READY;
    }

    @Override
    public boolean isOperationRedundant(Operation newOp) {
        if (newOp.getOperationType() != MARK_CONVERSATION_READ)
            return false;

        return ((MarkReadOperation) newOp).getConversationId().equals(getConversationId());
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return MARK_CONVERSATION_READ;
    }


    private void postSegmentMetrics(Response response) {
        String teamPrimaryConvId = ConversationContentProviderQueries.getTeamPrimaryConversationId(getContentResolver(), getConversationId());
        Properties segmentProperties = new SegmentService.PropertiesBuilder()
                .setNetworkResponse(response)
                .setSpaceIsTeam(!TextUtils.isEmpty(teamPrimaryConvId))
                .build();
        segmentService.reportMetric(SegmentService.READ_MESSAGES_EVENT, segmentProperties);
    }

}
