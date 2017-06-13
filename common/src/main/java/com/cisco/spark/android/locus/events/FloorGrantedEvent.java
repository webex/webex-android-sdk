package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class FloorGrantedEvent {
    private final LocusKey locusKey;

    public FloorGrantedEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
