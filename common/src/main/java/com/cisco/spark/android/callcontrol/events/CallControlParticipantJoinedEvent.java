package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;

import java.util.List;

public class CallControlParticipantJoinedEvent {
    private LocusKey locusKey;
    private final List<LocusParticipant> joinedParticipants;

    public CallControlParticipantJoinedEvent(LocusKey locusKey, List<LocusParticipant> joinedParticipants) {
        this.locusKey = locusKey;
        this.joinedParticipants = joinedParticipants;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public List<LocusParticipant> getJoinedParticipants() {
        return joinedParticipants;
    }
}
