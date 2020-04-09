package com.ciscowebex.androidsdk.internal.model;

import java.util.List;
import java.util.Map;

public class CalliopeClusterModel {

    private Map<String, Map<String, List<String>>> clusters;

    public Map<String, Map<String, List<String>>> getClusterInfo() {
        return clusters;
    }

}
