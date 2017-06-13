package com.cisco.spark.android.locus.events;


import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantDevice;

public class ParticipantPairedWithRoomSystemEvent {
    private LocusKey locusKey;
    private LocusParticipant participant;
    private LocusParticipantDevice device;
    private String associatedWith;

    public ParticipantPairedWithRoomSystemEvent(LocusKey locusKey, LocusParticipant participant, LocusParticipantDevice device, String associatedWith) {
        this.locusKey = locusKey;
        this.participant = participant;
        this.device = device;
        this.associatedWith = associatedWith;
    }

    public LocusKey getLocusKey() {
        return  locusKey;
    }

    public LocusParticipant getParticipant() {
        return participant;
    }

    public LocusParticipantDevice getDevice() {
        return device;
    }

    public String getAssociatedWith() {
        return associatedWith;
    }
}
