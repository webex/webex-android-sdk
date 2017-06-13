package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;


public class CallControlSelfParticipantLeftEvent {
    private final LocusKey locusKey;

    public CallControlSelfParticipantLeftEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
