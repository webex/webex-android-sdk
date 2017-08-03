package com.cisco.spark.android.whiteboard.renderer;

import android.view.MotionEvent;

public interface FrontBufferInterface {
    void clearFrontBufferTouches();
    void updateFrontBufferTouches(MotionEvent event);
}
