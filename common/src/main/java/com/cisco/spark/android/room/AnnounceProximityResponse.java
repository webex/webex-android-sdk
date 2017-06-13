package com.cisco.spark.android.room;

public class AnnounceProximityResponse {

    private int maxTokenValidityInSeconds;
    private int secondsToNextTokenEmit;

    public int getMaxTokenValidityInSeconds() {
        return maxTokenValidityInSeconds;
    }

    public int getSecondsToNextTokenEmit() {
        return secondsToNextTokenEmit;
    }

}
