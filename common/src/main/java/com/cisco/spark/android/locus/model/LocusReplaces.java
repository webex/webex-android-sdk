package com.cisco.spark.android.locus.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class LocusReplaces {
    @SerializedName("locusUrl")
    private LocusKey key;
    private Date lastActive;

    public LocusKey getLocusKey() {
        return key;
    }

    public Date getLastActive() {
        return lastActive;
    }
}
