/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
