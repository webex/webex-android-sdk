package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlFloorReleasedEvent {
    private LocusKey locusKey;

    public CallControlFloorReleasedEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
