package com.cisco.spark.android.presence;

public class QuietTimeStartedEvent {
    private int timeToLive;

    public QuietTimeStartedEvent(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public int getTimeToLive() {
        return timeToLive;
    }
}
