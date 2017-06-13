package com.cisco.spark.android.metrics;

import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.metrics.model.SplunkMetricItem;
import com.github.benoitdion.ln.Ln;

import java.util.Locale;

public class SplunkMetricsBuilder extends MetricsBuilder {
    public SplunkMetricsBuilder(MetricsEnvironment environment) {
        super(MetricsReportRequest.Endpoint.SPLUNK, environment);
    }

    public SplunkMetricsBuilder reportLocusTiming(String tag, int duration, String locusId) {
        SplunkMetricItem value = new SplunkMetricItem(locusId, tag, duration, environment.getName());
        Ln.d(String.format(Locale.US, "Timing: %s = %dms", tag, duration));
        request.addMetricValue(value);
        return this;
    }

    public SplunkMetricsBuilder reportTiming(String tag, int duration) {
        return reportLocusTiming(tag, duration, null);
    }

    public SplunkMetricsBuilder reportValue(String tag, Object value) {
        SplunkMetricItem item = new SplunkMetricItem(null, tag, value, environment.getName());
        request.addMetricValue(item);
        return this;
    }

    /**
     * This can be used to post to prod environment from a test build, to test your
     * metric code
     */
    public SplunkMetricsBuilder reportValue(String tag, Object value, MetricsEnvironment environment) {
        SplunkMetricItem item = new SplunkMetricItem(null, tag, value, environment.getName());
        request.addMetricValue(item);
        return this;
    }

    public SplunkMetricsBuilder reportThrottledValue(String tag, Object value) {
        return MetricsThrottler.shouldThrottle(tag, value) ? this : reportValue(tag, value);
    }
}
