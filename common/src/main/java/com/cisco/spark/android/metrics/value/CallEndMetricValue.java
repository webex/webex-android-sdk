package com.cisco.spark.android.metrics.value;

import android.net.Uri;

import com.cisco.spark.android.callcontrol.CallEndReason;
import com.cisco.spark.android.callcontrol.CallInitiationOrigin;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Date;

public class CallEndMetricValue {
    private String locusId;
    private boolean isCaller;
    private int audioStart;
    private int videoStart;
    private int clientCallDuration;
    private Uri deviceUrl;
    private Date locusTimestamp;
    private String participantId;
    private boolean isGroup;
    private boolean tcpFallback;
    private boolean cameraFailed;
    private boolean containsNonUsers;
    private boolean isNonStandard;
    private JsonObject mediaStatistics;
    private JsonObject packetStats;
    private String networkType;
    private boolean isZtm;
    private boolean isPaired;
    private String resourceType;
    private String resourceId;
    private String callOrigin;
    private boolean isSimulcast;
    private boolean isFilmstrip;
    private String joinLocusTrackingID;
    private String wmeVersion;
    private CallEndReason endReason;

    public CallEndMetricValue(String locusId, boolean isCaller, int audioStart, int videoStart,
                              int clientCallDuration, Uri deviceUrl, Date locusTimestamp, String participantId,
                              boolean isGroup, boolean isNonStandard, boolean isZtm, boolean isSimulcast, boolean isFilmstrip,
                              boolean containsNonUsers, boolean usedTcpFallback, boolean cameraFailed,
                              String packetStats, String networkType, Gson gson, String mediaStatistics,
                              boolean isPaired, String resourceType, String resourceId, CallInitiationOrigin callInitiationOrigin,
                              String joinLocusTrackingID, String wmeVersion, CallEndReason endReason) {
        this.locusId = locusId;
        this.isCaller = isCaller;
        this.audioStart = audioStart;
        this.videoStart = videoStart;
        this.clientCallDuration = clientCallDuration;
        this.deviceUrl = deviceUrl;
        this.locusTimestamp = locusTimestamp;
        this.participantId = participantId;
        this.isGroup = isGroup;
        this.tcpFallback = usedTcpFallback;
        this.cameraFailed = cameraFailed;
        this.containsNonUsers = containsNonUsers;
        this.isNonStandard = isNonStandard;
        this.networkType = networkType;
        this.isZtm = isZtm;
        this.isPaired = isPaired;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.joinLocusTrackingID = joinLocusTrackingID;
        this.wmeVersion = wmeVersion;
        this.endReason = endReason;

        if (callInitiationOrigin != null) {
            this.callOrigin = callInitiationOrigin.getValue();
        } else {
            this.callOrigin = CallInitiationOrigin.CallOriginationUnknown.getValue();
        }
        this.isSimulcast = isSimulcast;
        this.isFilmstrip = isFilmstrip;

        if (mediaStatistics != null) {
            JsonElement element = gson.fromJson(mediaStatistics, JsonElement.class);
            if (element != null) {
                JsonObject jsonObj = element.getAsJsonObject();
                this.mediaStatistics = jsonObj;
            }
        }

        if (packetStats != null) {
            JsonElement element = gson.fromJson(packetStats, JsonElement.class);
            if (element != null) {
                JsonObject jsonObj = element.getAsJsonObject();
                this.packetStats = jsonObj;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallEndMetricValue that = (CallEndMetricValue) o;

        if (audioStart != that.audioStart) return false;
        if (isCaller != that.isCaller) return false;
        if (videoStart != that.videoStart) return false;
        if (deviceUrl != null ? !deviceUrl.equals(that.deviceUrl) : that.deviceUrl != null) return false;
        if (locusId != null ? !locusId.equals(that.locusId) : that.locusId != null) return false;
        if (locusTimestamp != null ? !locusTimestamp.equals(that.locusTimestamp) : that.locusTimestamp != null)
            return false;
        if (participantId != null ? !participantId.equals(that.participantId) : that.participantId != null)
            return false;
        if (isGroup != that.isGroup) return false;
        if (tcpFallback != that.tcpFallback) return false;
        if (cameraFailed != that.cameraFailed) return false;
        if (containsNonUsers != that.containsNonUsers) return false;
        if (isNonStandard != that.isNonStandard) return false;
        if (isZtm != that.isZtm) return false;
        if (networkType != null ? !networkType.equals(that.networkType) : that.networkType != null) return false;
        if (isPaired != that.isPaired) return false;
        if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null)
            return false;
        if (resourceId != null ? !resourceId.equals(that.resourceId) : that.resourceId != null)
            return false;
        if (callOrigin != null ? !callOrigin.equals(that.callOrigin) : that.callOrigin != null)
            return false;
        if (isSimulcast != that.isSimulcast) return false;
        if (isFilmstrip != that.isFilmstrip) return false;
        if (wmeVersion != null ? !wmeVersion.equals(that.wmeVersion) : that.wmeVersion != null)
            return false;
        if (endReason != null ? !endReason.equals(that.endReason) : that.endReason != null)
            return false;

        return true;
    }
}
