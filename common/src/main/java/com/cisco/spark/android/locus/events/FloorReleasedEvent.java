package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class FloorReleasedEvent {
    private final LocusKey locusKey;
    private final String shareType;

    public FloorReleasedEvent(LocusKey locusKey, String shareType) {
        this.locusKey = locusKey;
        this.shareType = shareType;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public String getShareType() {
        return shareType;
    }
}

