package com.cisco.spark.android.events;

import com.cisco.spark.android.locus.model.LocusKey;


public class    CallNotificationRemoveEvent {
    private LocusKey locusKey;

    public CallNotificationRemoveEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
