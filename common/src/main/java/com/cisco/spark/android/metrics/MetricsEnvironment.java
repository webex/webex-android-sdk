package com.cisco.spark.android.metrics;

public enum MetricsEnvironment {
    ENV_TEST("TEST"),
    ENV_PROD(null);

    private String name;

    MetricsEnvironment(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
