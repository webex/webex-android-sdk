package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlJoinedMeetingFromLobbyEvent {

    private LocusKey locusKey;

    public CallControlJoinedMeetingFromLobbyEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
