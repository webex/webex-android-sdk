package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;

public class CallControlParticipantVideoMutedEvent {
    private final boolean isMuted;
    private final LocusParticipant participant;
    private final LocusKey locusKey;

    public CallControlParticipantVideoMutedEvent(LocusKey locusKey, LocusParticipant participant, boolean isMuted) {
        this.locusKey = locusKey;
        this.participant = participant;
        this.isMuted = isMuted;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public LocusParticipant getParticipant() {
        return participant;
    }
}
