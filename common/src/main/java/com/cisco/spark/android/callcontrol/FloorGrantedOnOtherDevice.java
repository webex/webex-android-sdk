package com.cisco.spark.android.callcontrol;

import com.cisco.spark.android.locus.model.LocusKey;

public class FloorGrantedOnOtherDevice {
    public LocusKey getLocusKey() {
        return locusKey;
    }

    private LocusKey locusKey;

    public FloorGrantedOnOtherDevice(LocusKey key) {
        locusKey = key;
    }
}
