package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.sync.SparkDatabaseQueries;
import com.cisco.spark.android.sync.operationqueue.MarkReadOperation;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;

import java.util.Date;

public class MessageMarkReadOperation extends MarkReadOperation {

    private String activityId;

    MessageMarkReadOperation(Injector injector, String spaceId) {
        super(injector, WebexId.translate(spaceId));
    }

    MessageMarkReadOperation(Injector injector, String spaceId, String messageId) {
        super(injector, WebexId.translate(spaceId));
        this.activityId = WebexId.translate(messageId);
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.acknowledge);
        long lastSelfAckTimestamp = SparkDatabaseQueries.getLastSelfAckTimestamp(getDatabaseProvider(), getConversationId());
        ActivityReference activityReference;
        if (null == activityId) {
            activityReference = SparkDatabaseQueries.getLastAckableActivity(getDatabaseProvider(), conversationId);
        } else {
            activityReference = SparkDatabaseQueries.getActivityReference(getDatabaseProvider(), activityId);
        }
        if (activityReference == null) {
            Ln.i("Failed marking conversation read because it has no activities");
            cancel();
            return;
        }
        if (lastSelfAckTimestamp >= activityReference.getPublishTime()) {
            Ln.d("Not sending an ack for conversation activity at time " + activityReference.getPublishTime() + " because we've already sent an ack for a more recent activity in this conversation");
            cancel();
            return;
        }
        Activity object = new Activity();
        object.setId(activityReference.getActivityId());
        object.setPublished(new Date(activityReference.getPublishTime()));
        object.setActivityType(activityReference.getType());
        activity.setObject(object);
    }

}
