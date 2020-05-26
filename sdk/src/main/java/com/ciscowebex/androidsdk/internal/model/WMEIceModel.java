package com.ciscowebex.androidsdk.internal.model;

import java.util.ArrayList;
import java.util.List;

public class WMEIceModel {
    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";

    private String iceFailed;
    private List<WMEIceResultModel> iceResults = new ArrayList<>();

    public String getIceFailed() {
        return iceFailed;
    }

    public void setIceFailed(String iceFailed) {
        this.iceFailed = iceFailed;
    }

    public List<WMEIceResultModel> getIceResults() {
        return iceResults;
    }

    public void setIceResults(List<WMEIceResultModel> iceResults) {
        this.iceResults = iceResults;
    }
}
