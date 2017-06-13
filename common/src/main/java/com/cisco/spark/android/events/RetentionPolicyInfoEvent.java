package com.cisco.spark.android.events;

import com.cisco.spark.android.model.RetentionPolicy;

public class RetentionPolicyInfoEvent {
    private RetentionPolicy retentionPolicy;

    public RetentionPolicyInfoEvent(RetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }
}
