package com.cisco.spark.android.metrics.value;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class CallStunTraceMetricValue {
    private String locusId;
    private Uri deviceUrl;
    private String locusTimestamp;
    private String requestTimestamp;
    private JsonObject detail;

    public CallStunTraceMetricValue(String locusId, Uri deviceUrl, String requestTimestamp, String locusTimestamp, String detailString, Gson gson) {
        this.locusId = locusId;
        this.deviceUrl = deviceUrl;
        this.requestTimestamp = requestTimestamp;
        this.locusTimestamp = locusTimestamp;

        JsonElement element = gson.fromJson(detailString, JsonElement.class);
        if (element != null) {
            JsonObject jsonObj = element.getAsJsonObject();
            this.detail = jsonObj;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallStunTraceMetricValue that = (CallStunTraceMetricValue) o;

        if (locusId != null ? !locusId.equals(that.locusId) : that.locusId != null) return false;
        if (deviceUrl != null ? !deviceUrl.equals(that.deviceUrl) : that.deviceUrl != null) return false;
        if (requestTimestamp != null ? !requestTimestamp.equals(that.requestTimestamp) : that.requestTimestamp != null)
        if (locusTimestamp != null ? !locusTimestamp.equals(that.locusTimestamp) : that.locusTimestamp != null)
            return false;
        if (detail != null ? !detail.equals(that.detail) : that.detail != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (locusId != null ? locusId.hashCode() : 0);
        result = 31 * result + (deviceUrl != null ? deviceUrl.hashCode() : 0);
        result = 31 * result + (locusTimestamp != null ? locusTimestamp.hashCode() : 0);
        result = 31 * result + (detail != null ? detail.hashCode() : 0);

        return result;
    }
}
