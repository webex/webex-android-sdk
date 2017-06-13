package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

import java.util.UUID;

public class ParticipantNotifiedEvent {

    private final LocusKey locusKey;
    private final UUID participantId;

    public ParticipantNotifiedEvent(LocusKey locusKey, UUID participantId) {
        this.locusKey = locusKey;
        this.participantId = participantId;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public UUID getUuid() {
        return participantId;
    }
}
