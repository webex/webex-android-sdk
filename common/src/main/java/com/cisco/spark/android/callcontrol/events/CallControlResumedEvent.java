package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlResumedEvent {
    private final LocusKey locusKey;

    public CallControlResumedEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
