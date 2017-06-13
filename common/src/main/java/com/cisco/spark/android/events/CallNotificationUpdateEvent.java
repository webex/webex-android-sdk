package com.cisco.spark.android.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallNotificationUpdateEvent {
    private LocusKey locusKey;
    private CallNotificationType type;
    private boolean booleanValue;

    public CallNotificationUpdateEvent(CallNotificationType type, LocusKey locusKey, boolean value) {
        this.locusKey = locusKey;
        this.type = type;
        this.booleanValue = value;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public CallNotificationType getType() {
        return type;
    }

    public boolean getValue() {
        return booleanValue;
    }
}
