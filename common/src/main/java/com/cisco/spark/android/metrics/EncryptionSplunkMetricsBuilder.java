package com.cisco.spark.android.metrics;

import com.cisco.spark.android.metrics.model.MetricsItem;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.metrics.value.EncryptionMetrics;
import com.github.benoitdion.ln.Ln;

public class EncryptionSplunkMetricsBuilder extends SplunkMetricsBuilder {

    public EncryptionSplunkMetricsBuilder(MetricsEnvironment environment) {
        super(environment);
    }

    public EncryptionSplunkMetricsBuilder reportValue(EncryptionMetrics.BaseMetric metric) {
        reportValue(metric.getKey(), metric);
        return this;
    }

    @Override
    public MetricsReportRequest build() {
        for (MetricsItem item : request.getMetrics()) {
            Ln.get("METRIC").v("Metric : " + item.getValue());
        }
        return super.build();
    }
}
