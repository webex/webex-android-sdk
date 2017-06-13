package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class NewLocusEvent {
    private final LocusKey locusKey;

    public NewLocusEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
