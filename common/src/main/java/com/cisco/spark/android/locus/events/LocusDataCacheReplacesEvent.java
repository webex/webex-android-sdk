package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class LocusDataCacheReplacesEvent {
    private LocusKey locusKey;
    private LocusKey replacedLocusKey;

    public LocusDataCacheReplacesEvent(LocusKey locusKey, LocusKey replacedLocusKey) {
        this.locusKey = locusKey;
        this.replacedLocusKey = replacedLocusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public LocusKey getReplacedLocusKey() {
        return replacedLocusKey;
    }
}
