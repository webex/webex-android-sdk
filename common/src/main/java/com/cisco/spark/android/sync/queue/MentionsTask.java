package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.util.LoggingUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

public class MentionsTask extends ActivitySyncTask {
    private long sinceDate;
    private int maxActivities;
    private boolean succeeded;

    @Inject
    ApiClientProvider apiClientProvider;

    public MentionsTask(Injector injector) {
        super(injector);
    }

    @Override
    public boolean execute() throws IOException {
        Ln.d("MentionsTask hitting mentions endpoint for max %d activities since time %s", maxActivities, String.valueOf(sinceDate));
        Response<ItemCollection<Activity>> response = apiClientProvider.getConversationClient().getUserMentions(sinceDate, maxActivities).execute();
        if (response.isSuccessful()) {
            succeeded = sync(response.body().getItems());
        } else {
            succeeded = false;
            Ln.w("Failed getting mentions: " + LoggingUtils.toString(response));
        }

        // This is called directly and not through the sync queue so we won't get this log unless we do it here
        Ln.i("Task Completed! " + this + " processed " + this.eventsProcessed + " activities in " + (System.currentTimeMillis() - this.timeStarted) + "ms. Requested at " + this.timeCreated);
        return succeeded;
    }

    public void setSinceDate(long sinceDate) {
        this.sinceDate = sinceDate;
    }

    public void setMaxActivities(int maxActivities) {
        this.maxActivities = maxActivities;
    }

    public boolean succeeded() {
        return succeeded;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("MentionsTask")
                .append(" id ")
                .append(taskId)
                .append(" fetch up to ")
                .append(String.valueOf(maxActivities))
                .append(" activities ")
                .append("since time ")
                .append(String.valueOf(sinceDate));

        return ret.toString();
    }
}
