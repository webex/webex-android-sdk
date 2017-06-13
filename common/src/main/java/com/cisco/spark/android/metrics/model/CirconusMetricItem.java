package com.cisco.spark.android.metrics.model;

public class CirconusMetricItem extends MetricsItem {
    private String type;

    public CirconusMetricItem(String key, Integer item, String env, String type) {
        super(key, item, env);
        this.type = type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CirconusMetricItem that = (CirconusMetricItem) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);

        return result;
    }
}
