package com.cisco.spark.android.locus.model;


public class LocusParticipantAudioControl  {
    private boolean muted;
    private LocusControlMeta meta;

    public LocusParticipantAudioControl(boolean muted, LocusControlMeta meta) {
        this.muted = muted;
        this.meta = meta;
    }

    public boolean isMuted() {
        return muted;
    }

    public LocusControlMeta getMeta() {
        return meta;
    }
}

