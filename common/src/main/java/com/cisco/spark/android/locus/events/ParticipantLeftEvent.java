package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;

import java.util.List;

public class ParticipantLeftEvent {
    private final LocusKey locusKey;
    private final Locus locus;
    private final List<LocusParticipant> leftParticipants;

    public ParticipantLeftEvent(LocusKey locusKey, Locus locus, List<LocusParticipant> leftParticipants) {
        this.locusKey = locusKey;
        this.locus = locus;
        this.leftParticipants = leftParticipants;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public Locus getLocus() {
        return locus;
    }

    public List<LocusParticipant> getLeftParticipants() {
        return leftParticipants;
    }

}
