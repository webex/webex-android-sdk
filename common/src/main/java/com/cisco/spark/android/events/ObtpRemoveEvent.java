package com.cisco.spark.android.events;


import com.cisco.spark.android.locus.model.LocusKey;

public class ObtpRemoveEvent {

    private LocusKey locusKey;

    public ObtpRemoveEvent(LocusKey locusKey) {
        this.locusKey = locusKey;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
