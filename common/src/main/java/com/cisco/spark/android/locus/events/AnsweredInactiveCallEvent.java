package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class AnsweredInactiveCallEvent {
    private final LocusKey locusKey;

    public AnsweredInactiveCallEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

}
