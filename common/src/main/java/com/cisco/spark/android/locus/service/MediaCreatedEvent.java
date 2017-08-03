package com.cisco.spark.android.locus.service;


import com.cisco.spark.android.locus.model.LocusKey;

public class MediaCreatedEvent {
    private final LocusKey locusKey;

    public MediaCreatedEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

}
