package com.cisco.spark.android.mercury.events;


import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;

import java.util.UUID;

public class KmsAckEvent extends MercuryData {
    private UUID requestId;
    private int statusCode;
    private String failureReason;

    public KmsAckEvent() {
        super();
        this.requestId = null;
        this.statusCode = 0;
        this.failureReason = "";
    }

    public KmsAckEvent(UUID requestId, int statusCode, String failureReason) {
        super(MercuryEventType.KMS_ACK);
        this.requestId = requestId;
        this.statusCode = statusCode;
        this.failureReason = failureReason;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

}
