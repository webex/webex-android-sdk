package com.cisco.spark.android.model;

import android.net.Uri;

public class MultistatusResponse {
    Uri href;
    ActivityData data;
    int status;
    String responseDescription;

    public Uri getHref() {
        return href;
    }

    public ActivityData getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public String getResponseDescription() {
        return responseDescription;
    }
}
