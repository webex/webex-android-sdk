package com.cisco.spark.android.metrics;

import com.cisco.spark.android.metrics.value.SpaceBindingMetricsValues;

public class RoomSystemMetricsBuilder extends SplunkMetricsBuilder {
    public static final String ANDROID_SPACE_IMPLICIT_BINDING = "android_space_implicit_binding";

    public  RoomSystemMetricsBuilder(MetricsEnvironment environment) {
        super(environment);
    }

    public RoomSystemMetricsBuilder addSpaceImplicitBinding(SpaceBindingMetricsValues.BindingMetricValue value) {
        return (RoomSystemMetricsBuilder) reportValue(ANDROID_SPACE_IMPLICIT_BINDING, value);
    }
}
