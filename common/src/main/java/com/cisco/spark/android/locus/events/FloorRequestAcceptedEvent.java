package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.MediaShare;

public class FloorRequestAcceptedEvent {
    private final LocusKey locusKey;
    private final String shareType;
    private final int httpStatusCode;

    public FloorRequestAcceptedEvent(LocusKey locusKey, int httpStatusCode, String shareType) {
        this.locusKey = locusKey;
        this.httpStatusCode = httpStatusCode;
        this.shareType = shareType;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public boolean isContent() {
        return shareType.equals(MediaShare.SHARE_CONTENT_TYPE);
    }

    public boolean isWhiteboard() {
        return shareType.equals(MediaShare.SHARE_WHITEBOARD_TYPE);
    }
}
