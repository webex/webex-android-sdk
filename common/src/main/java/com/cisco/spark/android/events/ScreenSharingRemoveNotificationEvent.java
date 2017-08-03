package com.cisco.spark.android.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class ScreenSharingRemoveNotificationEvent {
    private LocusKey locusKey;

    public ScreenSharingRemoveNotificationEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
