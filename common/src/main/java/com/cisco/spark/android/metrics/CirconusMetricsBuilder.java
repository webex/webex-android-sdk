package com.cisco.spark.android.metrics;

import com.cisco.spark.android.metrics.model.*;

public class CirconusMetricsBuilder extends MetricsBuilder {
    public CirconusMetricsBuilder(MetricsEnvironment environment) {
        super(MetricsReportRequest.Endpoint.CIRCONUS, environment);
    }

    public CirconusMetricsBuilder incrementCounter(String tag) {
        MetricsItem value = new CirconusMetricItem(tag, null, environment.getName(), MetricType.METRIC_TYPE_INCREMENT.getValue());
        request.addMetricValue(value);
        return this;
    }

    public CirconusMetricsBuilder decrementCounter(String tag) {
        MetricsItem value = new CirconusMetricItem(tag, null, environment.getName(), MetricType.METRIC_TYPE_DECREMENT.getValue());
        request.addMetricValue(value);
        return this;
    }

    public CirconusMetricsBuilder reportGauge(String tag, int meteredValue) {
        MetricsItem value = new CirconusMetricItem(tag, meteredValue, environment.getName(), MetricType.METRIC_TYPE_GAUGE.getValue());
        request.addMetricValue(value);
        return this;
    }

    public CirconusMetricsBuilder reportDuration(String tag, int duration) {
        MetricsItem value = new CirconusMetricItem(tag, duration, environment.getName(), MetricType.METRIC_TYPE_MSECS.getValue());
        request.addMetricValue(value);
        return this;
    }

    // Metric Types
    public enum MetricType {
        METRIC_TYPE_MSECS("MSECS"),
        METRIC_TYPE_GAUGE("GAUGE"),
        METRIC_TYPE_INCREMENT("INCREMENT"),
        METRIC_TYPE_DECREMENT ("DECREMENT"),
        METRIC_TYPE_STRING("STRING");

        private String value;

        private MetricType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
