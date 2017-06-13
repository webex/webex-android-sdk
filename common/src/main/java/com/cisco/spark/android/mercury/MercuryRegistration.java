package com.cisco.spark.android.mercury;

import com.cisco.spark.android.model.BufferState;
import com.cisco.spark.android.model.LocalClusterServiceUrls;

public class MercuryRegistration extends MercuryData {
    private BufferState bufferState;
    private LocalClusterServiceUrls localClusterServiceUrls;

    public BufferState getBufferState() {
        return bufferState;
    }

    public LocalClusterServiceUrls getLocalClusterServiceUrls() {
        return localClusterServiceUrls;
    }

    public MercuryRegistration() {
        super(MercuryEventType.MERCURY_REGISTRATION_STATUS);
    }
}
