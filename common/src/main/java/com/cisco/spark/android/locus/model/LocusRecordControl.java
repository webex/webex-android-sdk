package com.cisco.spark.android.locus.model;


public class LocusRecordControl {
    private boolean recording;
    private LocusControlMeta meta;

    public LocusRecordControl(boolean recording, LocusControlMeta meta) {
        this.recording = recording;
        this.meta = meta;
    }

    public boolean isRecording() {
        return recording;
    }

    public LocusControlMeta getMeta() {
        return meta;
    }
}
