package com.cisco.spark.android.metrics.model;

import java.util.ArrayList;
import java.util.List;

public class GenericMetricsRequest {
    private volatile List<GenericMetric> metrics = new ArrayList<>();

    public GenericMetricsRequest() {
    }

    public synchronized void addMetric(GenericMetric metric) {
        metrics.add(metric);
    }

    public synchronized void combine(GenericMetricsRequest otherRequest) {
        this.metrics.addAll(otherRequest.getMetrics());
    }

    public synchronized void addAllMetrics(List<GenericMetric> otherMetrics) {
        this.metrics.addAll(otherMetrics);
    }

    public synchronized List<GenericMetric> getMetrics() {
        // NOTE: Return a copy of the metrics
        return new ArrayList<>(metrics);
    }

    public synchronized int metricsSize() {
        return this.metrics.size();
    }
}
