package com.cisco.spark.android.core;

public interface Component {
    // Can Componet be started. Does it meet all the requirements for it start?
    public boolean shouldStart();
    // Start up, as in the beginning-of-time
    public void start();
    // Stop, as in the end-of-time. May be a start after this but not quiese nor activate.
    public void stop();
}
