package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.model.KeyObject;

import java.util.List;
import java.util.UUID;

public class KeyPushEvent extends MercuryData {
    private UUID requestId;
    private List<KeyObject> keys = null;
    private int statusCode = 0;
    private String failureReason = null;

    public KeyPushEvent() {
        super();
    }

    public KeyPushEvent(List<KeyObject> keys) {
        super(MercuryEventType.KEY_PUSH);
        this.keys = keys;

    }

    public UUID getRequestId() {
        return requestId;
    }

    public List<KeyObject> getKeys() {
        return this.keys;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getFailureReason() {
        return this.failureReason;
    }
}
