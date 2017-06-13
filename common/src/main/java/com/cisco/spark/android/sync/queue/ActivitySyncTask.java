package com.cisco.spark.android.sync.queue;


import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.github.benoitdion.ln.Ln;

import java.util.List;

/**
 * Abstract class for syncing activities that are pushed in via
 * endpoints such as mentions and search
 */
public abstract class ActivitySyncTask extends AbstractConversationSyncTask {

    private List<Activity> activitiesToBeSynced;

    protected ActivitySyncTask(Injector injector) {
        super(injector);
    }

    public boolean sync(List<Activity> activitiesToBeSynced) {
        this.activitiesToBeSynced = activitiesToBeSynced;
        if (this.activitiesToBeSynced != null) {
            Batch batch = batchProvider.get();

            // Add the activities to the DB.
            for (Activity activity : this.activitiesToBeSynced) {
                // The reason this should be SYNC_PARTIAL is that setting it to SYNC will set the high-water-mark for all conversations
                // to the latest activity and we don't want that because it would cause later catch-up syncs to miss messages
                activity.setSource(ConversationContract.ActivityEntry.Source.SYNC_PARTIAL);
                try {
                    // If we already have this activity, we don't have to do anything
                    if (ConversationContentProviderQueries.getActivityReference(getContentResolver(), activity.getId()) == null) {
                        super.addConversationActivity(batch, null, activity);
                        addGapRowsAroundActivity(batch, activity);
                    }
                } catch (Exception e) {
                    Ln.e(e, "Failed processing activity " + activity.getId() + " " + activity.getVerb());
                }
                encryptionDurationMetricManager.processActivity(activity);
                eventsProcessed++;
            }

            batch.apply();
        }
        return true;
    }

    @Override
    public String toString() {
        int syncSize = activitiesToBeSynced == null ? 0 : activitiesToBeSynced.size();
        StringBuilder ret = new StringBuilder(getClass().getSimpleName())
                .append(" id ")
                .append(taskId)
                .append(" sync ")
                .append(String.valueOf(syncSize))
                .append(" activities ");

        return ret.toString();
    }
}
