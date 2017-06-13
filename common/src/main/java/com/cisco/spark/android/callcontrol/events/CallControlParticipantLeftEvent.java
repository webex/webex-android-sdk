package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;

import java.util.List;

public class CallControlParticipantLeftEvent {
    private final LocusKey locusKey;
    private final List<LocusParticipant> leftParticipants;

    public CallControlParticipantLeftEvent(LocusKey locusKey, List<LocusParticipant> leftParticipants) {
        this.locusKey = locusKey;
        this.leftParticipants = leftParticipants;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public List<LocusParticipant> getLeftParticipants() {
        return leftParticipants;
    }
}
