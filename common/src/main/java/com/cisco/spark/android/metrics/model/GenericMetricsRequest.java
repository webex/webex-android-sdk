package com.cisco.spark.android.metrics.model;

import java.util.ArrayList;
import java.util.List;

public class GenericMetricsRequest {
    private List<GenericMetric> metrics;

    public GenericMetricsRequest() {
        metrics = new ArrayList<>();
    }

    public void addMetric(GenericMetric metric) {
        metrics.add(metric);
    }

    public void combine(GenericMetricsRequest otherRequest) {
        this.metrics.addAll(otherRequest.getMetrics());
    }

    public void addAllMetrics(List<GenericMetric> otherMetrics) {
        this.metrics.addAll(otherMetrics);
    }

    public List<GenericMetric> getMetrics() {
        return metrics;
    }

    public int metricsSize() {
        return this.metrics.size();
    }
}
