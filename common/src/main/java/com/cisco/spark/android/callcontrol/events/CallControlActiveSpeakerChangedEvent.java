package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusParticipant;

public class CallControlActiveSpeakerChangedEvent {

    private final LocusData call;
    private final LocusParticipant participant;
    private final long vid;

    public CallControlActiveSpeakerChangedEvent(LocusData call, LocusParticipant participant, long vid) {
        this.call = call;
        this.participant = participant;
        this.vid = vid;
    }

    public LocusData getCall() {
        return call;
    }

    public LocusParticipant getParticipant() {
        return participant;
    }

    public long getVid() {
        return vid;
    }
}
