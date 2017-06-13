package com.cisco.spark.android.model;

import android.net.Uri;

import java.util.List;

public class BulkActivitiesRequest {
    private List<Uri> activityUrls;

    public BulkActivitiesRequest(List<Uri> activityUrls) {
        this.activityUrls = activityUrls;
    }
}
