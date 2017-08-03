package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class FloorReleasedAcceptedEvent extends FloorRequestAcceptedEvent {

    public FloorReleasedAcceptedEvent(LocusKey locusKey, int httpStatusCode, String shareType) {
        super(locusKey, httpStatusCode, shareType);
    }
}
