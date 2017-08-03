package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class FloorReleasedDeniedEvent extends FloorRequestDeniedEvent {

    public FloorReleasedDeniedEvent(LocusKey locusKey, String errorMessage, int errorCode, String shareType) {
        super(locusKey, errorMessage, errorCode, shareType);
    }
}
