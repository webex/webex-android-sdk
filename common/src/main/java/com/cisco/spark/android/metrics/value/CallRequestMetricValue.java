package com.cisco.spark.android.metrics.value;

import android.net.Uri;

public class CallRequestMetricValue {
    private String locusId;
    private Uri deviceUrl;
    private String locusTimestamp;
    private String participantId;

    public CallRequestMetricValue(String locusId, Uri deviceUrl, String locusTimestamp, String participantId) {
        this.locusId = locusId;
        this.deviceUrl = deviceUrl;
        this.locusTimestamp = locusTimestamp;
        this.participantId = participantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallRequestMetricValue that = (CallRequestMetricValue) o;

        if (locusId != null ? !locusId.equals(that.locusId) : that.locusId != null) return false;
        if (deviceUrl != null ? !deviceUrl.equals(that.deviceUrl) : that.deviceUrl != null) return false;
        if (locusTimestamp != null ? !locusTimestamp.equals(that.locusTimestamp) : that.locusTimestamp != null)
            return false;
        if (participantId != null ? !that.participantId.equals(participantId) : (that.participantId) != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (locusId != null ? locusId.hashCode() : 0);
        result = 31 * result + (deviceUrl != null ? deviceUrl.hashCode() : 0);
        result = 31 * result + (locusTimestamp != null ? locusTimestamp.hashCode() : 0);
        result = 31 * result + (participantId != null ? participantId.hashCode() : 0);

        return result;
    }
}
