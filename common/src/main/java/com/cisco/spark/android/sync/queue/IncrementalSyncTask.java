package com.cisco.spark.android.sync.queue;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.SelfLeaveEvent;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.notification.HideConversationNotificationEvent;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationRecord;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.TitleBuilder;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.RxUtils;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import retrofit2.Response;
import rx.Observable;
import rx.functions.Action1;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry;

/**
 * Gets X most recent activity events across Y most recent conversations.
 *
 * This task only adds to the end of a conversation so it processes state-change activities
 */
public class IncrementalSyncTask extends AbstractConversationSyncTask {

    @Inject
    EncryptedConversationProcessor conversationProcessor;

    @Inject
    OperationQueue operationQueue;

    @Inject
    ApiClientProvider apiClientProvider;

    @Inject
    TitleBuilder titleBuilder;

    @Inject
    Provider<Batch> batchProvider;

    public static final int MAX_CONVERSATIONS_SHELL = 20;
    public static final int MAX_CONVERSATIONS = 9999;
    public static final int MAX_PARTICIPANTS = 100;

    public static final int HIGH_WATER_MARK = -1;

    protected long lastActivityPublishTime;
    protected int maxConversations;
    protected int maxActivities;
    protected long sinceTime;
    private int maxParticipants;

    HashSet<Uri> keysRequested = new HashSet<>();

    // use the factory methods in ActivitySyncQueue
    protected IncrementalSyncTask(Injector injector) {
        this(injector, false);
    }

    public IncrementalSyncTask(Injector injector, boolean healEncrypted) {
        super(injector);
    }

    protected Observable<Conversation> getConversations() throws IOException {
        return apiClientProvider.getConversationClient().getConversationsSince(Math.max(0, sinceTime - FUDGE_FACTOR), maxConversations, maxActivities, maxParticipants)
                .flatMap(new ConversationStreamObservableMapper(gson));
    }

    protected List<Conversation> getLeftConversations() throws IOException {
        Response<ItemCollection<Conversation>> response = apiClientProvider.getConversationClient().getConversationsLeftSince(Math.max(0, sinceTime - FUDGE_FACTOR), maxConversations).execute();
        if (response.isSuccessful()) {
            return response.body().getItems();
        }
        Ln.w("Failed getting LEFT conversations: " + LoggingUtils.toString(response));
        return Collections.EMPTY_LIST;
    }

    protected List<String> getConversationIds(List<Conversation> conversations) {
        List<String> ret = new ArrayList<>();
        for (Conversation conv : conversations) {
            ret.add(conv.getId());
        }
        return ret;
    }

    protected ConversationRecord getConversationRecord(Conversation conv) {
        return ConversationRecord.buildFromConversation(gson, conv, getSelf(), titleBuilder);
    }

    @Override
    public boolean execute() {

        if (sinceTime == HIGH_WATER_MARK)
            sinceTime = conversationSyncQueue.getHighWaterMark();

        try {
            final List<String> leftConversationIds = new ArrayList<>();

            final Batch batch = batchProvider.get();

            // If this is the initial sync don't ignore convs that have been
            // left and re-joined
            if (sinceTime > 0) {
                List<Conversation> leftConversations = getLeftConversations();
                leftConversationIds.addAll(getConversationIds(leftConversations));
                for (Conversation leftConv : leftConversations) {
                    leaveConversation(batch, leftConv);
                }
            }

            final ArrayList<String> conversationIds = new ArrayList<>();

            Action1<Conversation> onNextAction = new Action1<Conversation>() {
                @Override
                public void call(Conversation conv) {

                    Ln.i("Processing conversation " + conv);

                    conversationIds.add(conv.getId());

                    if (leftConversationIds.contains(conv.getId()))
                        return;

                    if (conv.isJoined() && hasSelfLeaveActivity(conv.getActivities().getItems())) {
                        leaveConversation(batch, conv);
                        eventBus.post(new SelfLeaveEvent());
                        return;
                    }

                    addConversationParticipants(batch, conv);

                    if (conv.getTeam() != null) {
                        addTeam(batch, conv.getTeam());
                    }

                    ConversationRecord cr = getConversationRecord(conv);

                    if (resultsIncludeCompleteParticipants(conv)) {
                        cr.setParticipantCount(conv.getParticipants().size());
                        cr.setParticipantsListValid(true);
                    }

                    List<Activity> activities = conv.getActivities().getItems();

                    processActivities(batch, getSelf(), conv, cr, activities);

                    addConversationMetadata(batch, cr);

                    if (batch.size() > 200) {
                        batch.apply();
                        batch.clear();
                    }

                    // This frees up memory on very large syncs
                    cr.freeParticipants();
                }
            };

            Observable<Conversation> convStream = getConversations();
            if (convStream != null) {
                convStream.subscribe(onNextAction, RxUtils.onError);
            } else {
                Ln.i("Failed getting conversation stream for incremental sync");
                return false;
            }

            if (conversationIds.size() < maxConversations && !conversationIds.isEmpty()) {
                setConversationListLoadingIndicator(batch, false);
            } else if (conversationIds.size() > 1) {
                setConversationListLoadingIndicator(batch, true);
            }

            if (!batch.apply())
                return false;

        } catch (Exception e) {
            Ln.e(e, "Failed getting conversation items");
            return false;
        }

        // The batch above may have happened across many transactions if it was very large.
        // In that case all the activities we entered are still in SYNC_PARTIAL source. Since we
        // succeeded, go ahead and promote the SYNC_PARTIAL ones to SYNC
        if (getTaskHighWaterMark() > 0) {
            conversationSyncQueue.setHighWaterMark(getTaskHighWaterMark());
            Batch b = batchProvider.get();
            b.add(ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                    .withValue(ActivityEntry.SOURCE.name(), ActivityEntry.Source.SYNC.ordinal())
                    .withSelection(ActivityEntry.SOURCE + "=? AND " + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " <= ?",
                            new String[]{String.valueOf(ActivityEntry.Source.SYNC_PARTIAL.ordinal()), String.valueOf(getTaskHighWaterMark())})
                    .build());
            b.apply();
        }

        try {
            actorRecordProvider.sync();
        } catch (RemoteException | OperationApplicationException e) {
            Ln.e(e, "Failed syncing actors");
        }

        if (shouldNotify()) {
            postEventsToBus();
        }

        Batch batch = batchProvider.get();

        for (ConversationRecord cr : updatedConversations) {
            // We can't do this earlier because Actors need to be synced.
            updateParticipantCounts(batch, cr);
            updateTopParticipants(batch, cr);
            addGapToTopIfNecessary(batch, cr);

            // Doing this outside the main transaction minimizes collisions
            cr.addUpdateLastReadableTimestamp(batch);
        }

        try {
            batch.apply();
        } catch (Exception e) {
            Ln.e(e);
            return false;
        }

        for (ConversationRecord cr : updatedConversations) {
            Boolean isRead = ConversationContentProviderQueries.isConversationRead(getContentResolver(), cr.getId());
            if (isRead == Boolean.TRUE) {
                eventBus.post(new HideConversationNotificationEvent(cr.getId()));
            }
        }

        searchManager.updateConversationSearch(updatedConversations, activityDecryptedEvent.getActivities());

        if (!keysToFetch.isEmpty())
            operationQueue.requestKeys(keysToFetch);

        sendEncryptionMetrics();

        return true;
    }

    private void updateParticipantCounts(Batch batch, ConversationRecord conversationRecord) {
        conversationRecord.setParticipantCount(ConversationContentProviderQueries.getConversationParticipantCount(getContentResolver(), conversationRecord.getId()));
        if (!getSelf().isConsumer())
            conversationRecord.setExternalParticipantCount(ConversationContentProviderQueries.getConversationExternalParticipantCount(getContentResolver(), conversationRecord.getId(), getSelf().getOrgId()));
        else
            conversationRecord.setExternalParticipantCount(0);
        conversationRecord.addCountUpdateOperations(batch);
    }

    private void updateTopParticipants(Batch batch, ConversationRecord conversationRecord) {
        String where = ConversationContract.vw_Participant.CONVERSATION_ID + "=?";
        ArrayList<String> args = new ArrayList<>();
        args.add(conversationRecord.getId());

        if (conversationRecord.getSyncOperationId() != null) {
            where += " OR " + ConversationContract.vw_Participant.CONVERSATION_ID + "=?";
            args.add(conversationRecord.getSyncOperationId());
        }

        ArrayList<ActorRecord> topParticipants = new ArrayList<>();
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    ConversationContract.vw_Participant.CONTENT_URI,
                    ConversationContract.vw_Participant.DEFAULT_PROJECTION,
                    where, args.toArray(new String[args.size()]),
                    ConversationContract.vw_Participant.LAST_ACTIVE_TIME + " DESC LIMIT " + (ConversationResolver.MAX_TOP_PARTICIPANTS + 1)
            );

            while (c != null && c.moveToNext() && topParticipants.size() < ConversationResolver.MAX_TOP_PARTICIPANTS) {
                ActorRecord ar = new ActorRecord(c);
                if (ar.getKey().isAuthenticatedUser(getSelf())) {
                    continue;
                }
                topParticipants.add(ar);
            }
        } finally {
            if (c != null)
                c.close();
        }

        ContentProviderOperation op =
                ContentProviderOperation.newUpdate(Uri.withAppendedPath(
                        ConversationEntry.CONTENT_URI, conversationRecord.getId()))
                        .withValue(ConversationEntry.TOP_PARTICIPANTS.name(), gson.toJson(topParticipants))
                        .build();
        batch.add(op);
    }

    private void addGapToTopIfNecessary(Batch batch, ConversationRecord conversationRecord) {
        ActivityReference activity = ConversationContentProviderQueries.getFirstNonLocalActivity(getContentResolver(), conversationRecord.getId());
        if (activity != null) {
            if (activity.getType() != ActivityEntry.Type.CREATE_CONVERSATION && activity.getType() != ActivityEntry.Type.BACKFILL_GAP) {
                addGapRowAtTime(batch, conversationRecord.getId(), activity.getPublishTime() - 1, ActivityEntry.Type.BACKFILL_GAP);
            }
        }
    }

    protected void processActivities(Batch batch, AuthenticatedUser self, Conversation conv, ConversationRecord cr, List<Activity> activities) {

        if (!activities.isEmpty()) {
            try {
                Collections.sort(activities, Activity.ASCENDING_PUBLISH_TIME_COMPARATOR);
            } catch (UnsupportedOperationException e) {
                // This can happen if the activities list is not modifiable
                if (activities.size() > 1) {
                    activities = new ArrayList<>();
                    activities.addAll(conv.getActivities().getItems());
                    Collections.sort(activities, Activity.ASCENDING_PUBLISH_TIME_COMPARATOR);
                }
            }

            Activity firstActivity = activities.get(0);
            Activity lastActivity = activities.get(activities.size() - 1);

            lastActivityPublishTime = Math.max(lastActivityPublishTime, lastActivity.getPublished().getTime());
            int numActivities = 0;

            HashMap<ActorRecord.ActorKey, Activity> lastActiveTime = new HashMap<ActorRecord.ActorKey, Activity>();

            if (cr != null && keyManager.getBoundKey(cr.getDefaultEncryptionKeyUrl()) != null) {
                if (UriUtils.equals(conv.getDefaultActivityEncryptionKeyUrl(), cr.getTitleKeyUrl())) {
                    cr.setAreTitleAndSummaryEncrypted(false);
                }
                if (UriUtils.equals(conv.getDefaultActivityEncryptionKeyUrl(), cr.getAvatarEncryptionKeyUrl())) {
                    cr.setIsAvatarEncrypted(false);
                }
            }

            for (Activity activity : activities) {
                // To prevent huge transactions, incremental syncs may be broken up into many smaller transactions.
                // The smaller transactions get a "SYNC_PARTIAL" source. They are promoted to SYNC at the end in
                // a final transaction. This prevents a failure partway through from polluting the hwm.
                if (activity.getSource() == ActivityEntry.Source.SYNC)
                    activity.setSource(ActivityEntry.Source.SYNC_PARTIAL);

                try {
                    if (!activity.isAcknowledgeActivity()) {
                        numActivities++;
                    }

                    super.addConversationActivity(batch, cr, activity);

                    ActorRecord.ActorKey actorKey = activity.getActor().getKey();
                    if (activity.isAckable(self)) {
                        lastActiveTime.put(actorKey, activity);
                    }

                    eventsProcessed++;
                } catch (Exception e) {
                    Ln.e(e, "Failed processing activity " + activity.getId() + " " + activity.getVerb());
                }
            }
            for (ActorRecord.ActorKey actorKey : lastActiveTime.keySet()) {
                setParticipantLastActive(batch, actorKey, conv, lastActiveTime.get(actorKey));
            }

            // Get the first non ack activity since acks will be included in our endpoint responses outside of the normal activity stream a lot of the time.
            // if all we have is acks, firstNonAckActivity will be null and we won't add any gaps (which is ok because ack activities aren't directly viewable
            // in the conversation stream)
            Activity firstNonAckActivity = null;
            Iterator<Activity> it = activities.iterator();
            while (firstNonAckActivity == null && it.hasNext()) {
                Activity currentActivity = it.next();

                if (currentActivity.getType() != ActivityEntry.Type.ACK)
                    firstNonAckActivity = currentActivity;
            }

            if (numActivities == maxActivities
                    && firstActivity.getSource().ordinal() <= ActivityEntry.Source.SYNC_PARTIAL.ordinal()
                    && firstNonAckActivity != null) {
                addGapRowBeforeActivity(batch, firstNonAckActivity);
                cr.setParticipantsListValid(resultsIncludeCompleteParticipants(conv));
            } else if (resultsIncludeCompleteParticipants(conv)) {
                cr.setParticipantsListValid(true);
            }

            if (activities.size() > 1
                    && firstActivity.getSource().ordinal() <= ActivityEntry.Source.SYNC_PARTIAL.ordinal()
                    && lastActivity.getSource().ordinal() <= ActivityEntry.Source.SYNC_PARTIAL.ordinal()) {

                if (firstNonAckActivity != null) {
                    removeGapsBetween(batch, conv.getId(), firstNonAckActivity.getPublished().getTime(), lastActivity.getPublished().getTime());
                }
            }
        }
    }

    private void leaveConversation(Batch batch, Conversation conv) {
        Ln.i("Left conversation " + conv.getId());

        if (conv.isOpen() || doesContainSelfLeaveForOpenTeamConversation(conv.getActivities().getItems())) {
            batch.add(ConversationContentProviderOperation.setConversationUnjoined(conv.getId()));
            batch.add(ConversationContentProviderOperation.clearConversationActivities(conv.getId()));
            batch.add(ConversationContentProviderOperation.clearConversationParticipantEntries(conv.getId()));
        } else {
            batch.add(ConversationContentProviderOperation.deleteConversation(conv.getId()));
        }
        batch.add(ConversationContentProviderOperation.deleteConversationSearch(conv.getId()));
        eventBus.post(new HideConversationNotificationEvent(conv.getId()));
        searchManager.deleteConversationSearchEntry(conv.getId());
    }

    private boolean doesContainSelfLeaveForOpenTeamConversation(List<Activity> activities) {
        for (Activity activity : activities) {
            if (activity.isSelfLeaveActivity(getSelf()) && ((Conversation) activity.getTarget()).isOpen()) {
                return true;
            }
        }
        return false;
    }

    protected void setConversationListLoadingIndicator(Batch batch, boolean isLoading) {
        ContentProviderOperation op = null;
        if (isLoading) {
            op = ContentProviderOperation.newInsert(ConversationEntry.CONTENT_URI)
                    .withValue(ConversationEntry.CONVERSATION_ID.name(), "BACKFILL_GAP")
                    .withValue(ConversationEntry.SORTING_TIMESTAMP.name(), 0)
                    .build();

        } else {
            op = ContentProviderOperation.newDelete(ConversationEntry.CONTENT_URI)
                    .withSelection(ConversationEntry.CONVERSATION_ID + "=?",
                            new String[]{"BACKFILL_GAP"}
                    ).build();
        }

        if (op != null)
            batch.add(op);
    }

    /**
     * Syncs that only affect a subset of conversations should override this method and return 0
     */
    protected long getTaskHighWaterMark() {
        return lastActivityPublishTime;
    }

    /**
     * Don't post notifications for a catch-up sync, it can get very spammy when recovering from
     * offline.
     *
     * Override this if a sync should ping the notification service.
     */
    protected boolean shouldNotify() {
        return false;
    }

    public IncrementalSyncTask withMaxConversations(int max) {
        maxConversations = max;
        return this;
    }

    public IncrementalSyncTask withMaxActivities(int max) {
        maxActivities = max;
        return this;
    }

    public IncrementalSyncTask withSinceTime(long msTime) {
        sinceTime = msTime;
        return this;
    }

    /**
     * Interate through the activities, which must be sorted by time. There may be multiple adds and
     * leaves in the set so return the last one.
     *
     * @param activities Iterable of activities sorted by ascending published_time
     * @return true if the self user has left the building
     */
    protected boolean hasSelfLeaveActivity(Iterable<Activity> activities) {
        boolean ret = false;
        for (Activity activity : activities) {
            try {
                if (activity.isSelfLeaveActivity(getSelf()))
                    ret = true;
                else if (activity.isAddParticipantSelf(getSelf()))
                    ret = false;
            } catch (Exception e) {
                Ln.e(false, e);
                Ln.v("Activity is probably malformed: " + activity);
            }
        }
        return ret;
    }

    public String toString() {
        StringBuilder ret = new StringBuilder(getClass().getSimpleName())
                .append(" id ")
                .append(taskId)
                .append(" limit ")
                .append(String.valueOf(maxParticipants))
                .append(" participants and ")
                .append(String.valueOf(maxActivities))
                .append(" activities from ")
                .append(maxConversations)
                .append(" conversations since ")
                .append(String.valueOf(sinceTime));

        return ret.toString();
    }

    public long getMaxConversations() {
        return maxConversations;
    }

    public IncrementalSyncTask withMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
        return this;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    @Override
    protected boolean resultsIncludeCompleteParticipants(Conversation conv) {
        return getMaxParticipants() == MAX_PARTICIPANTS
                && conv.getParticipants().size() < MAX_PARTICIPANTS
                && conv.getParticipants().size() > 0;
    }
}
