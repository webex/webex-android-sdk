package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;


public class LocusControlsChangedEvent {
    private final LocusKey locusKey;

    public LocusControlsChangedEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
