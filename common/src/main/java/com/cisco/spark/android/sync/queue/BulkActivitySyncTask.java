package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.github.benoitdion.ln.Ln;

import java.util.List;
import java.util.Locale;

public class BulkActivitySyncTask extends ActivitySyncTask {
    private List<Activity> activities;
    private boolean syncSucceeded = false;

    public BulkActivitySyncTask(Injector injector, List<Activity> activities) {
        super(injector);
        this.activities = activities;
    }

    @Override
    public boolean execute() {
        Ln.d("Executing BulkActivitySyncTask to sync " + activities.size() + " activities");
        this.syncSucceeded = sync(activities);
        Ln.i(String.format(Locale.US, "BulkActivitySyncTask %s for %d activities", this.syncSucceeded ? "succeeded" : "failed", this.activities.size()));
        return syncSucceeded;
    }

    public boolean hasSyncSucceeded() {
        return syncSucceeded;
    }
}
