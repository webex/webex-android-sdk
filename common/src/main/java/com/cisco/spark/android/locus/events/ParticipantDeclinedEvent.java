package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.mercury.events.DeclineReason;

public class ParticipantDeclinedEvent {
    private LocusKey locusKey;
    private DeclineReason reason;

    public ParticipantDeclinedEvent(LocusKey locusKey, DeclineReason reason) {
        this.locusKey = locusKey;
        this.reason = reason;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public DeclineReason getReason() {
        return reason;
    }
}
