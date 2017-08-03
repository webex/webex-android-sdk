package com.cisco.spark.android.lyra.model;

public class Token {
    private String value;
    private TimeFrame emit;
    private String startTime;
    private long durationInMillis;
    private long remainingValidityInSeconds;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TimeFrame getEmit() {
        return emit;
    }

    public void setEmit(TimeFrame emit) {
        this.emit = emit;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public long getDurationInMillis() {
        return durationInMillis;
    }

    public void setDurationInMillis(long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    public long getRemainingValidityInSeconds() {
        return remainingValidityInSeconds;
    }

    public void setRemainingValidityInSeconds(long remainingValidityInSeconds) {
        this.remainingValidityInSeconds = remainingValidityInSeconds;
    }
}
