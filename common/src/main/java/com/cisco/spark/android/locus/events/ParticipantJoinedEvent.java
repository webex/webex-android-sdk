package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;

import java.util.List;

public class ParticipantJoinedEvent {
    private final LocusKey locusKey;
    private final Locus locus;
    private final List<LocusParticipant> joinedParticipants;

    public ParticipantJoinedEvent(LocusKey locusKey, Locus locus, List<LocusParticipant> joinedParticipants) {
        this.locusKey = locusKey;
        this.locus = locus;
        this.joinedParticipants = joinedParticipants;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public Locus getLocus() {
        return locus;
    }

    public List<LocusParticipant> getJoinedParticipants() {
        return joinedParticipants;
    }

}
