package com.cisco.spark.android.callcontrol.events;

import android.graphics.Rect;

public class CallControlMediaDecodeSizeChangedEvent {
    private int vid;
    private Rect size;

    public CallControlMediaDecodeSizeChangedEvent(int vid, Rect newSize) {
        this.vid = vid;
        this.size = newSize;
    }
}
