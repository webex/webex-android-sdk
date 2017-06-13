package com.cisco.spark.android.sync.queue;

import android.net.Uri;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public abstract class ConversationActivityFillTask extends AbstractConversationSyncTask {
    public static final long NO_VALID_GAP_BOUND = -1;

    protected String conversationId;
    protected Uri gap;
    protected int maxActivities;
    private boolean succeeded;

    @Inject
    ApiClientProvider apiClientProvider;

    protected ConversationActivityFillTask(Injector injector) {
        super(injector);
    }

    @Override
    public boolean execute() throws IOException {
        ActivityReference gapActivity = ConversationContentProviderQueries.getActivityReference(
                getContentResolver(), gap.getLastPathSegment());

        if (gapActivity == null) {
            Ln.e("ERROR no gap with id " + gap.getLastPathSegment());
            succeeded = false;
            return succeeded;
        }

        conversationId = gapActivity.getConversationId();

        long gapBound = getGapBound(gapActivity.getPublishTime());

        if (gapBound != NO_VALID_GAP_BOUND) {
            fetchActivities(gapBound, gapActivity.getPublishTime());
        } else {
            succeeded = false;
            Ln.e("ConversationForwardFillTask: No valid gap bound time could be established, don't fetch anything");
        }

        succeeded = true;
        // This is called directly and not through the sync queue so we won't get this log unless we do it here
        Ln.i("Task Completed! " + this + " processed " + this.eventsProcessed + " activities in " + (System.currentTimeMillis() - this.timeStarted) + "ms. Requested at " + this.timeCreated);
        return succeeded;
    }

    private void fetchActivities(long gapBound, long gapTime) throws IOException {
        List<Activity> activities = fetchActivitiesFromServer(gapBound);

        if (activities == null || activities.isEmpty()) {
            return;
        }

        Collections.sort(activities, Activity.DESCENDING_PUBLISH_TIME_COMPARATOR);

        Activity earliestActivity = activities.get(activities.size() - 1);
        Activity latestActivity = activities.get(0);

        Batch batch = batchProvider.get();
        // Add the activities to the DB.
        for (Activity activity : activities) {
            // The reason this should be SYNC_PARTIAL is that setting it to SYNC will set the high-water-mark for all conversations
            // to the latest activity and we don't want that because it would cause later catch-up syncs to miss messages
            activity.setSource(ConversationContract.ActivityEntry.Source.SYNC_PARTIAL);
            try {
                super.addConversationActivity(batch, null, activity);
            } catch (Exception e) {
                Ln.e(e, "Failed processing activity " + activity.getId() + " " + activity.getVerb());
            }
            encryptionDurationMetricManager.processActivity(activity);
            eventsProcessed++;
        }

        // We want to remove all gaps from the time of our current gap until the latest/earliest activity we get back (depending on the gap type)
        long gapRemovalLowerBound = this instanceof ConversationForwardFillTask ? gapBound : earliestActivity.getPublished().getTime();
        long gapRemovalUpperBound = this instanceof ConversationBackFillTask ? gapBound : latestActivity.getPublished().getTime();

        removeGapsBetween(batch, conversationId, gapRemovalLowerBound, gapRemovalUpperBound);

        // Make sure we have a gap if we need one. >= because sometimes the server gives us some extra activities
        if (activities.size() >= maxActivities) {
            addGapsIfNecessary(batch, gapTime, earliestActivity, latestActivity);
        }

        batch.apply();
        searchManager.updateActivitySearch(activities);
        //TODO update content provider to notify on conversationActivity when activities change
        getContentResolver().notifyChange(ConversationContract.ConversationEntry.CONTENT_URI, null, true);

        sendEncryptionMetrics();
    }

    /**
     * Get the time we need to query the server for activities.
     *
     * @param gapPublishTime - publish time of the gap activity
     * @return - the time that will be used in the server query for activities, or NO_VALID_GAP_BOUND if
     * no suitable time can be determined
     */
    abstract long getGapBound(long gapPublishTime);

    /**
     * Fetch activities from the server. Query depends on the type of fill so implement this in child classes
     *
     * @param gapBound - time bound for server query (will query either before or after this time depending on the type of fill)
     */
    abstract List<Activity> fetchActivitiesFromServer(long gapBound) throws IOException;

    /**
     * Add a new gap if necessary. Again, placement is dependent on the fill type, so implement in child classes
     *
     * @param batch            - DB batch update to include the gap in
     * @param gapTime          - Publish time of gap activity
     * @param earliestActivity - the earliest activity that we've fetched
     * @param latestActivity   - the latest activity that we've fetched
     */
    abstract void addGapsIfNecessary(Batch batch, long gapTime, Activity earliestActivity, Activity latestActivity);

    public void setGap(Uri gap) {
        this.gap = gap;
    }

    public void setMaxActivities(int maxActivities) {
        this.maxActivities = maxActivities;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public abstract String toString();
}
