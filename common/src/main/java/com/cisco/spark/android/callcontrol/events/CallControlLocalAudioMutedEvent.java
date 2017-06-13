package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlLocalAudioMutedEvent {
    private final boolean isMuted;
    private final LocusKey locusKey;

    public CallControlLocalAudioMutedEvent(LocusKey locusKey, boolean isMuted) {
        this.locusKey = locusKey;
        this.isMuted = isMuted;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public boolean isMuted() {
        return isMuted;
    }
}
