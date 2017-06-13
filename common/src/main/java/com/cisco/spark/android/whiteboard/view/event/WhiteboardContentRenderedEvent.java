package com.cisco.spark.android.whiteboard.view.event;

import android.graphics.Bitmap;

public class WhiteboardContentRenderedEvent {
    private Bitmap bitmap;

    public WhiteboardContentRenderedEvent(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    public Bitmap getBitmap() {
        return bitmap;
    }
}


