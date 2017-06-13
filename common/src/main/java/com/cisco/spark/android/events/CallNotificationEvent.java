package com.cisco.spark.android.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallNotificationEvent {
    private LocusKey locusKey;
    private CallNotificationType type;
    private boolean isOneOnOne;
    private boolean isMediaLocal;

    public CallNotificationEvent(CallNotificationType type, LocusKey locusKey, boolean isOneOnOne, boolean isMediaLocal) {
        this.type = type;
        this.locusKey = locusKey;
        this.isOneOnOne = isOneOnOne;
        this.isMediaLocal = isMediaLocal;
    }

    public CallNotificationType getType() {
        return type;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public boolean isOneOnOne() {
        return isOneOnOne;
    }

    public boolean isMediaLocal() {
        return isMediaLocal;
    }
}
