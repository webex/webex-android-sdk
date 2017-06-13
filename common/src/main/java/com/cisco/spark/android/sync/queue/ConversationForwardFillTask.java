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

import retrofit2.Response;

/**
 * This task fills a given gap in a conversation forwards with the N oldest events.
 */
public class ConversationForwardFillTask extends ConversationActivityFillTask {

    public ConversationForwardFillTask(Injector injector) {
        super(injector);
    }

    @Override
    long getGapBound(long gapPublishTime) {
        ActivityReference lastActivityBeforeGap = ConversationContentProviderQueries
                .getLastReliableActivityBefore(getContentResolver(),
                        conversationId, gapPublishTime);

        if (lastActivityBeforeGap != null) {
            return lastActivityBeforeGap.getPublishTime();
        } else {
            Ln.e("ConversationForwardFillTask failed, couldn't get a valid activity before the gap to use as a time bound");
            return NO_VALID_GAP_BOUND;
        }
    }

    @Override
    List<Activity> fetchActivitiesFromServer(long gapBound) throws IOException {
        Ln.d("ConversationForwardFillTask hitting activities endpoint for max %d activities after time %s", maxActivities, String.valueOf(gapBound));
        Response<ItemCollection<Activity>> response = apiClientProvider.getConversationClient()
                .getConversationActivitiesImmediatelyAfter(conversationId, gapBound, maxActivities)
                .execute();

        if (response.isSuccessful()) {
            return response.body().getItems();
        }

        Ln.e("ERROR failed forwardfilling conversation; 0 activities returned. " + LoggingUtils.toString(response));
        return null;
   }

    @Override
    void addGapsIfNecessary(Batch batch, long gapTime, Activity earliestActivity, Activity latestActivity) {
        if (ConversationContentProviderQueries.getActivityReference(getContentResolver(), latestActivity.getId()) == null) {
            addGapRowAfterActivity(batch, latestActivity);
        } else {
            Ln.d("ConversationForwardFillTask: No gap needed, we've fetched activities we already have");
        }
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("ForwardFillTask")
                .append(" id ")
                .append(taskId)
                .append(" fetch up to ")
                .append(String.valueOf(maxActivities))
                .append(" activities ");

        if (gap != null) {
            ret.append("within gap ").append(gap.getLastPathSegment());
        }

        return ret.toString();
    }
}
