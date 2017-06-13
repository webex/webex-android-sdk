package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.MediaShare;

public class FloorRequestAcceptedEvent {

    private final String shareType;

    public FloorRequestAcceptedEvent(String shareType) {
        this.shareType = shareType;
    }

    public boolean isContent() {
        return shareType.equals(MediaShare.SHARE_CONTENT_TYPE);
    }

    public boolean isWhiteboard() {
        return shareType.equals(MediaShare.SHARE_WHITEBOARD_TYPE);
    }
}
