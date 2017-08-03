package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

/**
 * Success joining locus
 */
public class SuccessJoiningLocusEvent {
    private LocusKey locusKey;
    private String usingResource;

    public SuccessJoiningLocusEvent(LocusKey locusKey, String usingResource) {
        this.locusKey = locusKey;
        this.usingResource = usingResource;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public String getUsingResource() {
        return usingResource;
    }
}
