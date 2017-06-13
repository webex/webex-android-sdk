package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlJoinedMeetingEvent {

    private LocusKey locusKey;

    public CallControlJoinedMeetingEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
