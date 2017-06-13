package com.cisco.spark.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BulkActivitiesResponse {

    @SerializedName("multistatus")
    private List<MultistatusResponse> responses;

    public List<MultistatusResponse> getResponses() {
        return responses;
    }
}
