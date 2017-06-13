package com.cisco.spark.android.locus.model;

public class LocusParticipantChallengeControl {
    private boolean challenging;
    private LocusControlMeta meta;

    public boolean isChallenging() {
        return challenging;
    }

    public LocusControlMeta getMeta() {
        return meta;
    }
}
