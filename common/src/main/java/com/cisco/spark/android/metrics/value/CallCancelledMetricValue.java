package com.cisco.spark.android.metrics.value;

import android.net.Uri;


public class CallCancelledMetricValue {
    private String locusId;
    private Uri deviceUrl;
    private String locusTimestamp;
    private String requestTimestamp;
    private boolean isNonStandard;
    private boolean isGroup;
    private boolean userCancelled;
    private boolean iceFailed;
    private boolean cameraFailed;
    private boolean isSimulcast;
    private boolean isFilmstrip;
    private String resourceType;
    private String resourceId;
    private boolean serverRejected;

    public CallCancelledMetricValue(String locusId, Uri deviceUrl, String requestTimestamp, String locusTimestamp,
                                    boolean isGroup, boolean isNonStandard, boolean isSimulcast, boolean isFilmstrip,
                                    boolean userCancelled, boolean iceFailed, boolean cameraFailed,
                                    String resourceType, String resourceId, boolean serverRejected, boolean dummy) {
        this.locusId = locusId;
        this.deviceUrl = deviceUrl;
        this.requestTimestamp = requestTimestamp;
        this.locusTimestamp = locusTimestamp;
        this.isNonStandard = isNonStandard;
        this.isGroup = isGroup;
        this.userCancelled = userCancelled;
        this.iceFailed = iceFailed;
        this.cameraFailed = cameraFailed;
        this.isSimulcast = isSimulcast;
        this.isFilmstrip = isFilmstrip;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.serverRejected = serverRejected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallCancelledMetricValue that = (CallCancelledMetricValue) o;

        if (locusId != null ? !locusId.equals(that.locusId) : that.locusId != null) return false;
        if (deviceUrl != null ? !deviceUrl.equals(that.deviceUrl) : that.deviceUrl != null) return false;
        if (requestTimestamp != null ? !requestTimestamp.equals(that.requestTimestamp) : that.requestTimestamp != null)
        if (locusTimestamp != null ? !locusTimestamp.equals(that.locusTimestamp) : that.locusTimestamp != null)
            return false;
        if (isGroup != that.isGroup) return false;
        if (isNonStandard != that.isNonStandard) return false;
        if (isGroup != that.isGroup) return false;
        if (userCancelled != that.userCancelled) return false;
        if (iceFailed != that.iceFailed) return false;
        if (cameraFailed != that.cameraFailed) return false;
        if (isSimulcast != that.isSimulcast) return false;
        if (isFilmstrip != that.isFilmstrip) return false;
        if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) return false;
        if (resourceId != null ? !resourceId.equals(that.resourceId) : that.resourceId != null) return false;
        if (serverRejected != that.serverRejected) return false;

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
