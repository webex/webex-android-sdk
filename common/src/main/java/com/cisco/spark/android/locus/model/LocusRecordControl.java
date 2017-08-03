package com.cisco.spark.android.locus.model;


public class LocusRecordControl {
    private boolean recording;
    private boolean paused;
    private String errorCode;
    private LocusControlMeta meta;

    public boolean isRecording() {
        return recording;
    }

    public boolean isPaused() {
        return paused;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocusControlMeta getMeta() {
        return meta;
    }

    public boolean isAbleToRecord() {
        return meta != null && !meta.isReadOnly();
    }
}
