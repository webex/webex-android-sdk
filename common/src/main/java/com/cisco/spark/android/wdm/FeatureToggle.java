package com.cisco.spark.android.wdm;


import java.util.Date;

public final class FeatureToggle {
    private String key;
    private String val;
    private boolean mutable;
    private Date lastModified;
    private FeatureToggleType type;


    public FeatureToggle(String key, String value, boolean mutable) {
        this.key = key;
        this.val = value;
        this.mutable = mutable;
    }

    public String getKey() {
        return key;
    }

    public boolean getBooleanVal() {
        return Boolean.parseBoolean(val);
    }

    public String getVal() {
        return val;
    }

    public int getIntVal() {
        return Integer.parseInt(val);
    }

    public boolean isMutable() {
        return mutable;
    }

    public boolean isBoolean() {
        return "true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val);
    }

    public Date getLastModified() {
        return lastModified;
    }

    public FeatureToggleType getType() {
        return type;
    }

    public void setType(FeatureToggleType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeatureToggle that = (FeatureToggle) o;

        if (!key.equals(that.key)) return false;

        return true;
    }

    @Override
    public String toString() {
        return "FeatureToggle{" +
                "key='" + key + '\'' +
                ", val='" + val + '\'' +
                ", mutable=" + mutable +
                ", type=" + type +
                ", lastModified=" + lastModified +
                '}';
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
