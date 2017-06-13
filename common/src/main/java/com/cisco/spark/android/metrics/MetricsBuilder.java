package com.cisco.spark.android.metrics;

import com.cisco.spark.android.metrics.model.*;

public abstract class MetricsBuilder {
    protected final MetricsReportRequest request;
    protected final MetricsEnvironment environment;

    protected MetricsBuilder(MetricsReportRequest.Endpoint endpoint, MetricsEnvironment environment) {
        this.request = new MetricsReportRequest(endpoint);
        this.environment = environment;
    }

    public MetricsReportRequest build() {
        return request;
    }

    public MetricsReportRequest build(String name) {
        request.setName(name);
        return build();
    }
}
