package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.ObjectType;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Response;

public class FetchActivityContextTask extends AbstractConversationSyncTask {
    private String conversationId;
    private int maxActivities;
    private long timestamp;
    private boolean succeeded;

    @Inject
    ApiClientProvider apiClientProvider;

    public FetchActivityContextTask(Injector injector) {
        super(injector);
    }

    @Override
    public boolean execute() throws IOException {
        Ln.d("FetchActivityContextTask fetching up to %s messages for conversation %s around time %s", String.valueOf(maxActivities), conversationId, String.valueOf(timestamp));
        Response<ItemCollection<Activity>> response = apiClientProvider.getConversationClient().getConversationActivitiesAround(conversationId, timestamp, maxActivities).execute();
        List<Activity> activities = null;
        if (response.isSuccessful()) {
            activities = response.body().getItems();
        }

        if (activities == null || activities.isEmpty()) {
            Ln.e("ERROR failed fetching activity context; 0 activities returned. " + LoggingUtils.toString(response));
            return false;
        }

        Collections.sort(activities, Activity.ASCENDING_PUBLISH_TIME_COMPARATOR);

        // We need to ignore content update activities for timestamp purposes since we won't have all activities between them,
        // so add them to their own list and grab the first and last one for gap calculation purposes
        List<Activity> nonUpdateActivities = new ArrayList<>();
        for (Activity activity : activities) {
            if (!(Verb.update.equals(activity.getVerb()) && ObjectType.content.equals(activity.getObjectType()))) {
                nonUpdateActivities.add(activity);
            }
        }

        Collections.sort(nonUpdateActivities, Activity.ASCENDING_PUBLISH_TIME_COMPARATOR);

        Activity earliestNonUpdateActivity = nonUpdateActivities.get(0);
        Activity latestNonUpdateActivity = nonUpdateActivities.get(nonUpdateActivities.size() - 1);

        Batch batch = batchProvider.get();

        // Add the activities to the DB
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

        Ln.d("FetchActivityContextTask finished processing %d activities", activities.size());

        if (activities.size() > 1) {
            Ln.d("FetchActivityContextTask removing gaps %s-%s", String.valueOf(earliestNonUpdateActivity.getPublished().getTime()), String.valueOf(latestNonUpdateActivity.getPublished().getTime()));
            removeGapsBetween(batch, conversationId, earliestNonUpdateActivity.getPublished().getTime(), latestNonUpdateActivity.getPublished().getTime());
        }

        // Make sure we have gaps if we need them. >= because sometimes the server gives us some extra activities
        if (activities.size() >= maxActivities) {
            if (ConversationContentProviderQueries.getActivityReference(getContentResolver(), earliestNonUpdateActivity.getId()) == null) {
                addGapRowBeforeActivity(batch, earliestNonUpdateActivity);
            } else {
                Ln.d("FetchActivityContextTask: No backfill gap needed, we've fetched activities we already have");
            }

            if (ConversationContentProviderQueries.getActivityReference(getContentResolver(), latestNonUpdateActivity.getId()) == null) {
                addGapRowAfterActivity(batch, earliestNonUpdateActivity);
            } else {
                Ln.d("FetchActivityContextTask: No forwardfill gap needed, we've fetched activities we already have");
            }
        }

        batch.apply();
        searchManager.updateActivitySearch(activities);
        //TODO update content provider to notify on conversationActivity when activities change
        getContentResolver().notifyChange(ConversationContract.ConversationEntry.CONTENT_URI, null, true);

        sendEncryptionMetrics();
        succeeded = true;
        return succeeded;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setMaxActivities(int maxActivities) {
        this.maxActivities = maxActivities;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean succeeded() {
        return succeeded;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("FetchActivityContextTask")
                .append(" id ")
                .append(taskId)
                .append(" fetch up to ")
                .append(String.valueOf(maxActivities))
                .append(" activities around time ")
                .append(String.valueOf(timestamp))
                .append(" for conversation ")
                .append(conversationId);

        return ret.toString();
    }
}
