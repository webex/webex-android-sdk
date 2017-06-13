package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

/**
 * This task fills a given gap in a conversation backwards with the N most recent events.
 */
public class ConversationBackFillTask extends ConversationActivityFillTask {

    public ConversationBackFillTask(Injector injector) {
        super(injector);
    }

    @Override
    long getGapBound(long gapPublishTime) {
        ActivityReference firstActivityAfterGap = ConversationContentProviderQueries
                .getFirstReliableActivityAfter(getContentResolver(),
                        conversationId, gapPublishTime);

        if (firstActivityAfterGap != null) {
            return firstActivityAfterGap.getPublishTime();
        } else {
            return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        }
    }

    @Override
    List<Activity> fetchActivitiesFromServer(long gapBound) throws IOException {
        Ln.d("ConversationBackFillTask hitting activities endpoint for max %d activities before time %s", maxActivities, String.valueOf(gapBound));
        Response<ItemCollection<Activity>> response = apiClientProvider.getConversationClient()
                .getConversationActivitiesBefore(conversationId, gapBound, maxActivities)
                .execute();

        if (response.isSuccessful()) {
            return response.body().getItems();
        }

        Ln.e("ERROR failed backfilling conversation; 0 activities returned. " + LoggingUtils.toString(response));
        return null;
    }

    @Override
    void addGapsIfNecessary(Batch batch, long gapTime, Activity earliestActivity, Activity latestActivity) {
        if (ConversationContentProviderQueries.getActivityReference(getContentResolver(), earliestActivity.getId()) == null) {
            addGapRowBeforeActivity(batch, earliestActivity);
        } else {
            Ln.d("ConversationBackFillTask: No gap needed, we've fetched activities we already have");
        }
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("BackFillTask")
                .append(" id ")
                .append(taskId)
                .append(" fetch up to ")
                .append(String.valueOf(maxActivities))
                .append(" activities ");

        if (gap != null) {
            ret.append("within gap ").append(gap.getLastPathSegment());
        } else {
            ret.append("since time 0");
        }

        return ret.toString();
    }
}
