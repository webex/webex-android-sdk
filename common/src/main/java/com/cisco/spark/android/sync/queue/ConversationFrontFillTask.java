package com.cisco.spark.android.sync.queue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;
import rx.Observable;

public class ConversationFrontFillTask extends IncrementalSyncTask {

    private Conversation conversation;
    protected String conversationId;

    public ConversationFrontFillTask(Injector injector, String conversationId) {
        super(injector);
        this.conversationId = conversationId;
        this.sinceTime = 0;
        this.maxConversations = 1;
        this.maxActivities = 20;
        // note MAX_PARTICIPANTS is not really a numeric limit for this task, the endpoint takes a
        // boolean for whether to include them. MAX_PARTICIPANTS == true, else false.
        this.withMaxParticipants(MAX_PARTICIPANTS);
    }

    public ConversationFrontFillTask(Injector injector, Conversation conversation) {
        this(injector, conversation.getId());
        this.conversation = conversation;
    }

    @Override
    protected Observable<Conversation> getConversations() throws IOException {
        if (conversation != null)
            return Observable.just(conversation);

        Response<Conversation> response = apiClientProvider.getConversationClient()
                .getMostRecentConversationActivitiesSince(conversationId, 0,
                        maxActivities,
                        getMaxParticipants() == MAX_PARTICIPANTS ? "true" : "false").
                        execute();

        if (response.isSuccessful()) {
            Conversation ret = response.body();
            for (Activity activity : ret.getActivities().getItems()) {
                // The reason this should be <SYNC is that setting it to SYNC will set the high-water-mark for all conversations
                // to the latest activity and we don't want that because it would cause later catch-up syncs to miss messages.
                // Also note that we are not SYNC_PARTIAL because this is not a catch-up sync and we don't want to trigger
                // gap logic here.
                activity.setSource(ConversationContract.ActivityEntry.Source.SHELL);
            }
            return Observable.just(ret);
        } else {
            Ln.w("Failed getting conversation for frontfill: " + LoggingUtils.toString(response));
        }
        return null;
    }

    @Override
    protected List<Conversation> getLeftConversations() {
        return Collections.emptyList();
    }

    @Override
    protected boolean shouldNotify() {
        return true;
    }

    @Override
    protected long getTaskHighWaterMark() {
        // We're only getting one conversation, doesn't count against hwm
        return 0;
    }

    @Override
    protected boolean resultsIncludeCompleteParticipants(Conversation conv) {
        return getMaxParticipants() == MAX_PARTICIPANTS;
    }

    @Override
    protected void setConversationListLoadingIndicator(Batch batch, boolean isLoading) {
    }

    protected String addGapRowBeforeActivity(@NonNull Batch batch, @NonNull Activity activity) {
        // Don't add a backfill gap if we already have the activity
        if (ConversationContentProviderQueries.getActivityReference(getContentResolver(), activity.getId()) == null) {
            return super.addGapRowBeforeActivity(batch, activity);
        }

        return null;
    }

    protected String addGapRowAfterActivity(@NonNull Batch batch, @NonNull Activity activity) {
        return null;
    }

}
