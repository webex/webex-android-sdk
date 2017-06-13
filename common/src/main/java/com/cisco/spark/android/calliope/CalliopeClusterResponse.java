package com.cisco.spark.android.calliope;

import android.support.v4.util.ArrayMap;

import java.util.List;
import java.util.Map;

public class CalliopeClusterResponse {

    private int statusCode;
    private Map<String, Cluster> clusters;

    public CalliopeClusterResponse() {
        clusters = new ArrayMap<>();
    }

    public Map<String, Cluster> getClusterInfo() {
        return clusters;
    }

    public static class Cluster {

        List<String> udp;
        List<String> tcp;
        List<String> https;
        List<String> xtls;

    }
}

