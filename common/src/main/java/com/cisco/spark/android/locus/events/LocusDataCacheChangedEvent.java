package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class LocusDataCacheChangedEvent {
    public enum Type {
        ADDED,
        REMOVED,
        MODIFIED
    }

    private LocusKey locusKey;
    private Type change;

    public LocusDataCacheChangedEvent(LocusKey locusKey, Type change) {
        this.locusKey = locusKey;
        this.change = change;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public Type getLocusChange() {
        return change;
    }
}
