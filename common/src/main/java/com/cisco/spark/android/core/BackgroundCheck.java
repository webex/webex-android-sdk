package com.cisco.spark.android.core;

public interface BackgroundCheck {
    void start();

    void stop();

    boolean isInBackground();

    boolean isInteractive();

    boolean waitForActiveState(long msTimeToWait);

}
