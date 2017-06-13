package com.cisco.spark.android.app;

import android.content.*;

import java.util.*;

public class AndroidActivityManager implements ActivityManager {
    private final Context context;
    private final android.app.ActivityManager delegate;

    public AndroidActivityManager(Context context, android.app.ActivityManager delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    public boolean isMostRecentTask() {
        List<android.app.ActivityManager.RecentTaskInfo> tasks = delegate.getRecentTasks(1, 1);
        if (tasks != null && !tasks.isEmpty()) {
            String recentPkg = tasks.get(0).baseIntent.getComponent().getPackageName();
            return recentPkg.equals(context.getPackageName());
        }
        return false;
    }
}
