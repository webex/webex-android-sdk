package com.cisco.spark.android.events;

import android.graphics.Bitmap;

public class ForceLoadBoardEvent {

    private final String channelId;
    private final Bitmap backgroundBitmap;

    public ForceLoadBoardEvent(String channelId, Bitmap backgroundBitmap) {
        this.channelId = channelId;
        this.backgroundBitmap = backgroundBitmap;
    }

    public String getChannelId() {
        return channelId;
    }

    public Bitmap getBackgroundBitmap() {
        return backgroundBitmap;
    }
}

