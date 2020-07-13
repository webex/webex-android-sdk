package com.ciscowebex.androidsdk.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class GenericMetricsModel {

    private final LinkedBlockingDeque<GenericMetricModel> metrics = new LinkedBlockingDeque<>();
    private final transient Object sync = new Object();

    public GenericMetricsModel() {
    }

    public GenericMetricsModel(List<GenericMetricModel> metrics) {
        this();
        addAllMetrics(metrics);
    }

    public void addMetric(GenericMetricModel metric) {
        synchronized (sync) {
            metrics.add(metric);
        }
    }

    public void combine(GenericMetricsModel otherRequest) {
        synchronized (sync) {
            this.metrics.addAll(otherRequest.getMetrics());
        }
    }

    public void addAllMetrics(List<GenericMetricModel> otherMetrics) {
        synchronized (sync) {
            this.metrics.addAll(otherMetrics);
        }
    }

    public List<GenericMetricModel> getMetrics() {
        synchronized (sync) {
            // return a copy of the metrics
            return new ArrayList<>(metrics);
        }
    }

    public ArrayList<GenericMetricModel> popMetrics(int n) {
        ArrayList<GenericMetricModel> ret = new ArrayList<>();
        synchronized (sync) {
            metrics.drainTo(ret, n);
        }
        return ret;
    }

    public int metricsSize() {
        synchronized (sync) {
            return this.metrics.size();
        }
    }
}
