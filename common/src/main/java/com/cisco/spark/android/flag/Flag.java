package com.cisco.spark.android.flag;

import android.net.Uri;

import com.cisco.spark.android.util.UriUtils;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Flag {
    private String id;

    @SerializedName("flag-item")
    private Uri flagItem;

    private String state;

    @SerializedName("date-updated")
    private Date dateUpdated;

    @SerializedName("date-created")
    private Date dateCreated;

    public static Flag flagRequest(Uri activityUrl) {
        Flag flag = new Flag();
        flag.flagItem = activityUrl;
        flag.dateUpdated = new Date();
        flag.state = "flagged";
        return flag;
    }

    public String getId() {
        return id;
    }

    public Date getDateUpdated() {
        return dateUpdated != null ? dateUpdated : dateCreated;
    }

    public String getActivityId() {
        return UriUtils.extractUUID(flagItem).toString();
    }
}
