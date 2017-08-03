package com.cisco.spark.android.locus.requests;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.Floor;

public class FloorShareRequest extends DeltaRequest {

    private final Floor floor;
    private final String resourceUrl;

    public FloorShareRequest(CoreFeatures coreFeatures, Floor floor) {
        super(coreFeatures);
        this.floor = floor;
        this.resourceUrl = null;
    }

    public FloorShareRequest(CoreFeatures coreFeatures, Floor floor, String resourceUrl) {
        super(coreFeatures);
        this.floor = floor;
        this.resourceUrl = resourceUrl;
    }

    public Floor getFloor() {
        return floor;
    }

}
