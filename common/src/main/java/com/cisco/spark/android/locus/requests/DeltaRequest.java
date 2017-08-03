package com.cisco.spark.android.locus.requests;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.LocusSequenceInfo;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;


public class DeltaRequest {
    private LocusSequenceInfo sequence;

    DeviceRegistration deviceRegistration;

    public DeltaRequest(CoreFeatures coreFeatures) {
        if (coreFeatures != null && coreFeatures.isDeltaEventEnabled()) {
            Ln.d("sequence initialized to enable delta events for: %s", getClass().getSimpleName());
            sequence = new LocusSequenceInfo();
        }
    }
}
