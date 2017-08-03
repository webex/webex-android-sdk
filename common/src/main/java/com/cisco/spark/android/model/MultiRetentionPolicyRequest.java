package com.cisco.spark.android.model;

import java.util.List;

public class MultiRetentionPolicyRequest {

    private List<String> retentionUrls;

    public MultiRetentionPolicyRequest(List<String> retentionUrls) {
        this.retentionUrls = retentionUrls;
    }

}
