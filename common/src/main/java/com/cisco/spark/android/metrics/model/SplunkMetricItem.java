package com.cisco.spark.android.metrics.model;

public class SplunkMetricItem extends MetricsItem {
    private String cid;

    public SplunkMetricItem(String cid, String key, Object value, String env) {
        super(formatSplunkTag(key), value, env);
        this.cid = cid;
    }

    private static String formatSplunkTag(String tag) {
        return tag.replace('.', '_');
    }

    public String getCid() {
        return cid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SplunkMetricItem that = (SplunkMetricItem) o;

        if (cid != null ? !cid.equals(that.cid) : that.cid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (cid != null ? cid.hashCode() : 0);
        return result;
    }

    // helper fn, eventually refactor so all metric types have their own classes; that way values can be strong-typed
    public int getIntValue() {
        if (getValue() != null && getValue() instanceof Integer) {
            return (Integer) getValue();
        }
        return 0;
    }
}
