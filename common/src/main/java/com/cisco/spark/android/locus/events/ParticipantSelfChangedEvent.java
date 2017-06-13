package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusKey;

public class ParticipantSelfChangedEvent {
    private final LocusKey locusKey;
    private final Locus locus;

    public ParticipantSelfChangedEvent(LocusKey locusKey, Locus locus) {
        this.locusKey = locusKey;
        this.locus = locus;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public Locus getLocus() {
        return locus;
    }
}
