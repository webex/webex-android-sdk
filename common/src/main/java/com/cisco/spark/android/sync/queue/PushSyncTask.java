package com.cisco.spark.android.sync.queue;

import android.content.ContentResolver;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationRecord;
import com.github.benoitdion.ln.Ln;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import rx.Observable;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry;

/**
 * PushSyncTask - Task to handle work coming in from the ActivitySyncQueue
 */
public class PushSyncTask extends IncrementalSyncTask {

    private final ActivitySyncQueue activitySyncQueue;

    protected PushSyncTask(Injector injector, ActivitySyncQueue activitySyncQueue) {
        super(injector);
        this.activitySyncQueue = activitySyncQueue;
    }

    @Override
    protected ConversationRecord getConversationRecord(Conversation conv) {
        ConversationRecord ret = ConversationRecord
                .buildFromContentResolver(getContentResolver(),
                        gson, conv.getId(), titleBuilder);

        if (ret == null) {
            ret = ConversationRecord.buildFromConversation(gson, conv, getSelf(), titleBuilder);
        } else {
            ret.applyConversationObject(conv);
        }
        return ret;
    }

    @Override
    protected Observable<Conversation> getConversations() {
        Collection<Conversation> conversations = activitySyncQueue.getWork();

        if (!conversations.isEmpty()) {
            HashSet<Conversation> missingConversations = new HashSet<Conversation>();

            for (Conversation conversation : conversations) {
                // If not a known convo, we might want to kick off a frontfill task
                if (!isKnownConversation(getContentResolver(), conversation.getId())
                        // If one of the new activities is us leaving the convo, don't bother
                        && !hasSelfLeaveActivity(conversation.getActivities().getItems())
                        // PushSyncs with source==LOCAL are outgoing so don't trigger a frontfill
                        && !isLocalEdit(conversation.getActivities().getItems())) {
                    missingConversations.add(conversation);
                    continue;
                }
            }

            for (Conversation missingConv : missingConversations) {
                // New conversations don't come through Mercury quite right (missing title) so
                // fall back to a front-fill sync for the conv.
                Ln.d("Conversation " + missingConv.getId() + " is not known; fetching latest");
                conversationSyncQueue.submitPriorityTask(conversationSyncQueue.getConversationFrontFillTask(missingConv.getId()));
                conversations.remove(missingConv);
            }
        } else {
            Ln.e("Work is empty");
        }
        return Observable.from(conversations);
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

    public String toString() {
        StringBuilder ret = new StringBuilder(getClass().getSimpleName())
                .append(" id ")
                .append(taskId);

        return ret.toString();
    }

    public static boolean isKnownConversation(ContentResolver contentResolver, String conversationId) {
        String knownConvId = ConversationContentProviderQueries.getOneValue(contentResolver,
                ConversationEntry.CONVERSATION_ID,
                ConversationEntry.CONVERSATION_ID + "=? AND " + ConversationEntry.SELF_JOINED + "=1 ",
                new String[]{conversationId});

        return TextUtils.equals(conversationId, knownConvId);
    }

    private boolean isLocalEdit(List<Activity> activities) {
        for (Activity activity : activities) {
            if (activity.getSource() == ActivityEntry.Source.LOCAL)
                return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotify() {
        return true;
    }

    @Override
    protected boolean resultsIncludeCompleteParticipants(Conversation conv) {
        return false;
    }
}
