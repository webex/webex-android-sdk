package com.cisco.spark.android.metrics.value;

import android.net.Uri;


public class CallAlertMetricValue {
    private String locusId;
    private Uri deviceUrl;
    private String locusTimestamp;
    private String requestTimestamp;
    private boolean isCaller;
    private boolean isNonStandard;
    private boolean isGroup;
    private String source;

    public CallAlertMetricValue(String locusId, Uri deviceUrl, String requestTimestamp, String locusTimestamp, boolean isCaller,
                                boolean isGroup, boolean isNonStandard, String source) {
        this.locusId = locusId;
        this.deviceUrl = deviceUrl;
        this.requestTimestamp = requestTimestamp;
        this.locusTimestamp = locusTimestamp;
        this.isCaller = isCaller;
        this.isNonStandard = isNonStandard;
        this.isGroup = isGroup;
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallAlertMetricValue that = (CallAlertMetricValue) o;

        if (locusId != null ? !locusId.equals(that.locusId) : that.locusId != null) return false;
        if (deviceUrl != null ? !deviceUrl.equals(that.deviceUrl) : that.deviceUrl != null) return false;
        if (requestTimestamp != null ? !requestTimestamp.equals(that.requestTimestamp) : that.requestTimestamp != null)
        if (locusTimestamp != null ? !locusTimestamp.equals(that.locusTimestamp) : that.locusTimestamp != null)
            return false;
        if (isCaller != that.isCaller) return false;
        if (isGroup != that.isGroup) return false;
        if (isNonStandard != that.isNonStandard) return false;
        if (isCaller != that.isCaller) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (locusId != null ? locusId.hashCode() : 0);
        result = 31 * result + (deviceUrl != null ? deviceUrl.hashCode() : 0);
        result = 31 * result + (locusTimestamp != null ? locusTimestamp.hashCode() : 0);

        return result;
    }
}
