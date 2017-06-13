package com.cisco.spark.android.events;

public class CoachmarkCenterLocationsUpdatedEvent {
    private int[][] cutoutCenters;

    public int[][] getCutoutCenters() {
        return cutoutCenters;
    }

    public void setCutoutCenters(int[][] cutoutCenters) {
        this.cutoutCenters = cutoutCenters;
    }
}
