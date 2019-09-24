package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.sync.SparkDatabaseQueries;
import com.cisco.spark.android.sync.operationqueue.MarkReadOperation;
import com.github.benoitdion.ln.Ln;

import java.util.Date;

public class MarkMessageReadOption extends MarkReadOperation {
    private String messageId;
    public MarkMessageReadOption(Injector injector, String conversationId) {
        super(injector, conversationId);
    }

    public MarkMessageReadOption(Injector injector, String conversationId, String messageId){
        super(injector, conversationId);
        this.messageId = messageId;
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.acknowledge);
        long lastSelfAckTimestamp = SparkDatabaseQueries.getLastSelfAckTimestamp(getDatabaseProvider(), getConversationId());
        ActivityReference activityReference;
        if (null == messageId){
            activityReference = SparkDatabaseQueries.getLastAckableActivity(getDatabaseProvider(), conversationId);
        }else {
            activityReference = SparkDatabaseQueries.getActivityReference(getDatabaseProvider(), messageId);
        }
        if (activityReference == null) {
            Ln.i("Failed marking conversation read because it has no activities");
            cancel();
            return;
        }
        if (lastSelfAckTimestamp >= activityReference.getPublishTime()) {
            Ln.d("Not sending an ack for conversation activity at time " + String.valueOf(activityReference.getPublishTime()) + " because we've already sent an ack for a more recent activity in this conversation");
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
