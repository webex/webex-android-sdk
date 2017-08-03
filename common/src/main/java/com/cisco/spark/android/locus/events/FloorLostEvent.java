package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.MediaShare;

public class FloorLostEvent {
    private final LocusKey locusKey;
    private final MediaShare localMediaShare;
    private final MediaShare remoteMediaShare;

    public FloorLostEvent(LocusKey locusKey, MediaShare localMediaShare, MediaShare remoteMediaShare) {
        this.locusKey = locusKey;
        this.localMediaShare = localMediaShare;
        this.remoteMediaShare = remoteMediaShare;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public MediaShare getLocalMediaShare() {
        return localMediaShare;
    }

    public MediaShare getRemoteMediaShare() {
        return remoteMediaShare;
    }
}
