package com.cisco.spark.android.mercury;

public abstract class MercuryData {
    private MercuryEventType eventType;

    public MercuryData() {
    }

    public MercuryData(MercuryEventType type) {
        this.eventType = type;
    }

    public MercuryEventType getEventType() {
        return eventType;
    }
}
