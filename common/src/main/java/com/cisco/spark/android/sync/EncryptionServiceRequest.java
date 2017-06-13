package com.cisco.spark.android.sync;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class EncryptionServiceRequest {
    private static final long REQUEST_TIME_THRESHOLD = TimeUnit.SECONDS.toMillis(15);
    private UUID requestId;
    private long requestTime;

    public EncryptionServiceRequest() {
        this.requestId = UUID.randomUUID();
        this.requestTime = System.currentTimeMillis();
    }

    public long getRequestTime() {
        return this.requestTime;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public boolean isRequestTimeBelowThreshold() {
        if (this.requestTime == 0) {
            return false;
        } else {
            long diff = System.currentTimeMillis() - this.requestTime;
            return diff <= REQUEST_TIME_THRESHOLD;
        }
    }
}
