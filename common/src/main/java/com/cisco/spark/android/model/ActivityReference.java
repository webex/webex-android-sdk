package com.cisco.spark.android.model;

import android.database.Cursor;

import com.cisco.spark.android.sync.ConversationContract;

/**
 * A minimal set of properties on an activity
 */
public class ActivityReference {
    String activityId;
    String actorId;
    String conversationId;
    ConversationContract.ActivityEntry.Type type;
    long publishTime;
    ConversationContract.ActivityEntry.Source source;
    private String data;

    /**
     * Construct a basic reference to an activity
     * @param c Cursor populated with ActivityEntry.DEFAULT_PROJECTION
     */
    public ActivityReference(Cursor c) {

        if (c.isBeforeFirst())
            c.moveToFirst();

        activityId = c.getString(ConversationContract.ActivityEntry.ACTIVITY_ID.ordinal());
        actorId = c.getString(ConversationContract.ActivityEntry.ACTOR_ID.ordinal());
        conversationId = c.getString(ConversationContract.ActivityEntry.CONVERSATION_ID.ordinal());
        publishTime = c.getLong(ConversationContract.ActivityEntry.ACTIVITY_PUBLISHED_TIME.ordinal());
        type = ConversationContract.ActivityEntry.Type.values()[c.getInt(ConversationContract.ActivityEntry.ACTIVITY_TYPE.ordinal())];
        source = ConversationContract.ActivityEntry.Source.values()[c.getInt((ConversationContract.ActivityEntry.SOURCE.ordinal()))];
        data = c.getString(ConversationContract.ActivityEntry.ACTIVITY_DATA.ordinal());
    }

    public long getPublishTime() {
        return publishTime;
    }

    public ConversationContract.ActivityEntry.Type getType() {
        return type;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getActorId() {
        return actorId;
    }

    public ConversationContract.ActivityEntry.Source getActivitySource() {
        return source;
    }

    public String getData() {
        return data;
    }
}
