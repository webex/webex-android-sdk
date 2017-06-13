package com.cisco.spark.android.locus.events;

import com.cisco.spark.android.locus.model.MediaShare;


public class FloorRequestDeniedEvent extends RetrofitErrorEvent {

    private String shareType;

    public FloorRequestDeniedEvent(String errorMessage, int errorCode, String shareType) {
        super(errorMessage, errorCode);
        this.shareType = shareType;
    }

    public boolean isContent() {
        return shareType.equals(MediaShare.SHARE_CONTENT_TYPE);
    }

    public boolean isWhiteboard() {
        return shareType.equals(MediaShare.SHARE_WHITEBOARD_TYPE);
    }
}
