package com.cisco.spark.android.metrics.value;

import android.net.Uri;

public class CallJoinMetricValue {
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    private String locusId;
    private Uri deviceUrl;
    private String usingResource;
    private String result;
    private String errorMessage;

    public CallJoinMetricValue(String locusId, Uri deviceUrl, String usingResource, String result, String errorMessage) {
        this.locusId = locusId;
        this.deviceUrl = deviceUrl;
        this.usingResource = usingResource;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallJoinMetricValue that = (CallJoinMetricValue) o;

        if (locusId != null ? !locusId.equals(that.locusId) : that.locusId != null) return false;
        if (deviceUrl != null ? !deviceUrl.equals(that.deviceUrl) : that.deviceUrl != null)
            return false;
        if (usingResource != null ? !usingResource.equals(that.usingResource) : that.usingResource != null) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (locusId != null ? locusId.hashCode() : 0);
        result = 31 * result + (deviceUrl != null ? deviceUrl.hashCode() : 0);

        return result;
    }
}
