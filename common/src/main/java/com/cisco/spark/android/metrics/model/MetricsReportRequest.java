package com.cisco.spark.android.metrics.model;

import java.util.*;

public class MetricsReportRequest {
    // Metrics endpoint
    public enum Endpoint {
        CIRCONUS,
        SPLUNK
    }

    private List<MetricsItem> metrics;
    private Endpoint endpoint;
    private String name;

    public MetricsReportRequest(Endpoint endpoint) {
        this.metrics = new ArrayList<MetricsItem>();
        this.endpoint = endpoint;
    }

    public void addMetricValue(MetricsItem value) {
        metrics.add(value);
    }

    public List<MetricsItem> getMetrics() {
        return this.metrics;
    }

    public Endpoint getEndpoint() {
        return this.endpoint;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
