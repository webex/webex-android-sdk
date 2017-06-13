package com.cisco.spark.android.locus.model;


public class LocusLockControl {
    private boolean locked;
    private LocusControlMeta meta;

    public LocusLockControl(boolean locked, LocusControlMeta meta) {
        this.locked = locked;
        this.meta = meta;
    }

    public boolean isLocked() {
        return locked;
    }

    public LocusControlMeta getMeta() {
        return meta;
    }
}
