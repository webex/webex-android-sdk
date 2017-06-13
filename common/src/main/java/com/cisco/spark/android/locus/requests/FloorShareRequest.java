package com.cisco.spark.android.locus.requests;

import com.cisco.spark.android.locus.model.Floor;

public class FloorShareRequest {

    private final Floor floor;
    private final String resourceUrl;

    public FloorShareRequest(Floor floor) {
        this.floor = floor;
        this.resourceUrl = null;
    }

    public FloorShareRequest(Floor floor, String resourceUrl) {
        this.floor = floor;
        this.resourceUrl = resourceUrl;
    }

    public Floor getFloor() {
        return floor;
    }

}
