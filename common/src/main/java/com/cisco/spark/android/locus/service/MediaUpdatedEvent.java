package com.cisco.spark.android.locus.service;

import com.cisco.spark.android.locus.model.LocusKey;

public class MediaUpdatedEvent {
    private final LocusKey locusKey;

    public MediaUpdatedEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
