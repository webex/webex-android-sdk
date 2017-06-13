package com.cisco.spark.android.util;

public class SystemClock implements Clock {
    @Override
    public long now() {
        return System.currentTimeMillis();
    }

    @Override
    public long monotonicNow() {
        return System.nanoTime() / 1000000;
    }
}
