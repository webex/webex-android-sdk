package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.MediaShare;


public class FloorRequestDeniedEvent extends RetrofitErrorEvent {
    private LocusKey locusKey;
    private String shareType;

    public FloorRequestDeniedEvent(LocusKey locusKey, String errorMessage, int errorCode, String shareType) {
        super(errorMessage, errorCode);
        this.locusKey = locusKey;
        this.shareType = shareType;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public boolean isContent() {
        return shareType.equals(MediaShare.SHARE_CONTENT_TYPE);
    }

    public boolean isWhiteboard() {
        return shareType.equals(MediaShare.SHARE_WHITEBOARD_TYPE);
    }
}
