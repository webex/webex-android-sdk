package com.cisco.spark.android.metrics.model;

public class MetricsItem {
    private String key;
    private Object value;
    private String env;

    public MetricsItem(String key, Object value, String env) {
        this.key = key;
        this.value = value;
        this.env = env;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public String getEnv() {
        return env;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricsItem that = (MetricsItem) o;

        if (env != null ? !env.equals(that.env) : that.env != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (env != null ? env.hashCode() : 0);
        return result;
    }
}
