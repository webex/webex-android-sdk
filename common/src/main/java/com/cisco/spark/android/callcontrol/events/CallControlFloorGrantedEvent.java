package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlFloorGrantedEvent {
    private final LocusKey locusKey;

    public CallControlFloorGrantedEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
