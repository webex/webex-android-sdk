package com.cisco.spark.android.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class ScreenSharingNotificationEvent {
    private String title;
    private LocusKey locusKey;
    private boolean isOneOnOne;
    private boolean isMediaLocal;

    public ScreenSharingNotificationEvent(String title, LocusKey locusKey, boolean isOneOnOne, boolean isMediaLocal) {
        this.title = title;
        this.locusKey = locusKey;
        this.isOneOnOne = isOneOnOne;
        this.isMediaLocal = isMediaLocal;
    }

    public String getTitle() {
        return title;
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
