package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationRecord;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import rx.Observable;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry.ACTIVITY_PUBLISHED_TIME;

/**
 * Task for adding an outgoing activity to the DB.
 * <p/>
 * This is called synchronously from Operation.onEnqueue so it is guaranteed to complete before the
 * activity is sent to the server.
 */
public class NewActivityTask extends IncrementalSyncTask {
    private Activity activity;

    public NewActivityTask(Injector injector, Activity activity) {
        super(injector);
        this.activity = activity;
        sinceTime = 0;
    }

    @Override
    public boolean execute() {

        // Timestamp handling for locally posted activities is special because the device clock is
        // not reliable, and 'most-recent' timestamps can only move forward.
        //
        // For the timestamps in the conversation table that govern read/unread and
        // sorting order, use minimal values that give the expected results. They will be overwritten
        // when the activities are echoed back from the server with guaranteed correct timestamps.

        ConversationRecord cr = ConversationRecord.buildFromContentResolver(getContentResolver(), gson, activity.getConversationId(), titleBuilder);
        long tempTimestamp = ConversationContentProviderQueries
                .getOneLongValue(
                        getContentResolver(),
                        ActivityEntry.CONTENT_URI,
                        "MAX (" + ACTIVITY_PUBLISHED_TIME + ")",
                        ActivityEntry.SOURCE + " < " + ActivityEntry.Source.LOCAL.ordinal(), null);

        if (cr != null) {
            tempTimestamp = Math.max(tempTimestamp, cr.getSortingTimestamp());
            tempTimestamp = Math.max(tempTimestamp, cr.getLastReadableActivityDateRemote().getTime());
        }
        tempTimestamp += 100;

        activity.setPublished(new Date(tempTimestamp));

        return super.execute();
    }

    @Override
    protected Observable<Conversation> getConversations() {
        Conversation conversation = new Conversation(activity.getConversationId());
        conversation.getActivities().addItem(activity);

        if (activity.getTarget() instanceof Conversation) {
            Conversation conv = (Conversation) activity.getTarget();
            conversation.setIsOpen(conv.isOpen());
            conversation.setIsTeam(conv.isTeam());
        }

        return Observable.just(conversation);
    }

    @Override
    protected ConversationRecord getConversationRecord(Conversation conv) {
        ConversationRecord ret = ConversationRecord.buildFromContentResolver(getContentResolver(), gson, conv.getId(), titleBuilder);
        if (ret == null)
            ret = ConversationRecord.buildFromConversation(gson, conv, getSelf(), titleBuilder);
        return ret;
    }

    @Override
    protected List<Conversation> getLeftConversations() {
        return Collections.emptyList();
    }

    @Override
    protected long getTaskHighWaterMark() {
        return 0;
    }

    @Override
    protected void setConversationListLoadingIndicator(Batch batch, boolean isLoading) {
    }

    @Override
    protected boolean resultsIncludeCompleteParticipants(Conversation conv) {
        return false;
    }

    @Override
    protected boolean shouldNotify() {
        return false;
    }
}
