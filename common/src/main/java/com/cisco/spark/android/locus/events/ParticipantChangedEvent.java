package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

import java.util.UUID;

/**
 * Generic event for a property of a participant changing in locus DTO
 */
public class ParticipantChangedEvent {

    private final LocusKey locusKey;
    private final UUID participantId;

    public ParticipantChangedEvent(LocusKey locusKey, UUID participantId) {
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
