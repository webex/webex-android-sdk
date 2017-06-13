package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantDevice;


public class ParticipantUnPairedWithRoomSystemEvent {
    private LocusKey locusKey;
    private LocusParticipant participant;
    private LocusParticipantDevice device;

    public ParticipantUnPairedWithRoomSystemEvent(LocusKey locusKey, LocusParticipant participant, LocusParticipantDevice device) {
        this.locusKey = locusKey;
        this.participant = participant;
        this.device = device;
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
}
