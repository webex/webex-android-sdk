package com.cisco.spark.android.events;

import com.cisco.spark.android.model.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityDecryptedEvent {
    private final List<Activity> activities = new ArrayList<Activity>();

    public ActivityDecryptedEvent() {
    }

    public ActivityDecryptedEvent(Activity activity) {
        this.activities.add(activity);
    }

    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    public List<Activity> getActivities() {
        return activities;
    }

    @Override
    public String toString() {
        return "Event: " + activities.toString();
    }
}
