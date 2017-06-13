package com.cisco.spark.android.metrics;

import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.callcontrol.CallEndReason;
import com.cisco.spark.android.callcontrol.CallInitiationOrigin;
import com.cisco.spark.android.metrics.value.AudioControlMetricValue;
import com.cisco.spark.android.metrics.value.CallAlertMetricValue;
import com.cisco.spark.android.metrics.value.CallEndMetricValue;
import com.cisco.spark.android.metrics.value.CallNumericDialPrevented;
import com.cisco.spark.android.metrics.value.CallRequestMetricValue;
import com.cisco.spark.android.metrics.value.CallStunTraceMetricValue;
import com.cisco.spark.android.metrics.value.CallToggleAudioOnlyMetricValue;
import com.cisco.spark.android.util.DateUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.Date;

public class CallMetricsBuilder extends SplunkMetricsBuilder {
    public static final String CALL_END_TAG = "callEnd";
    public static final String CALL_REQUEST_TAG = "callRequest";
    public static final String CALL_ALERT_TAG = "callAlert";
    public static final String CALL_QUALITY_TAG = "meetup_call_user_rating";
    public static final String CALL_STUN_TRACE_TAG = "callStunTrace";
    public static final String CALL_NUMERIC_DIAL_PREVENTED = "callNumericDialPrevented";
    public static final String MUTE_CONTROL = "lyraMuteControl";
    public static final String VOLUME_CONTROL = "lyraVolumeControl";
    public static final String MUTE_ACTION = "lyraMuteAction";
    public static final String UNMUTE_ACTION = "lyraUnMuteAction";
    public static final String VOLUME_ACTION = "lyraVolumeAction";
    public static final String CALL_SWITCH_TO_AUDIO_ONLY = "callSwitchToAudioOnly";
    public static final String CALL_SWITCH_TO_VIDEO = "callSwitchToVideo";

    public CallMetricsBuilder(MetricsEnvironment environment) {
        super(environment);
    }

    public CallMetricsBuilder addCallMetrics(String locusId, long startTime, Date audioTime, Date videoTime, boolean inboundCall,
                                             boolean isGroupMeetup, boolean usedTcpFallback, boolean cameraFailed, boolean containsNonUser,
                                             boolean isNonStandard, boolean isZtm, boolean isPaired, boolean isSimulcast, boolean isFilmstrip,
                                             Date requestTimestamp, int participants, String participantID, String mediaStatistics,
                                             String packetStatistics, String networkType, String resourceType, String resourceId,
                                             Gson gson, Uri deviceUrl, CallInitiationOrigin callInitiationOrigin,
                                             String joinLocusTrackingID, String wmeVersion, CallEndReason endReason) {
        long audioStart = 0, videoStart = 0;
        long endTime = new Date().getTime();
        int callToAudio, callToVideo, callToAV, callDuration;

        if (audioTime == null) {
            callToAudio = MetricsReporter.NO_EVENT;
        } else {
            audioStart = audioTime.getTime();
            // we could start receiving media before call has 'connected' (i.e. 2 or more people on call)
            // this would lead to audioStart value that's less than startTime.  Similarly for videoStart below
            if (audioStart < startTime) {
                startTime = audioStart;
            }
            callToAudio = (int) (audioStart - startTime);
        }

        if (videoTime == null) {
            callToVideo = MetricsReporter.NO_EVENT;
        } else {
            videoStart = videoTime.getTime();
            if (videoStart < startTime) {
                startTime = videoStart;
            }
            callToVideo = (int) (videoStart - startTime);
        }

        callDuration = (int) (endTime - startTime);
        if ((audioTime != null) && (videoTime != null)) {
            if (audioStart > videoStart) {
                callToAV = callToAudio;
            } else {
                callToAV = callToVideo;
            }
        } else {
            callToAV = MetricsReporter.NO_EVENT;
        }

        if (callToAudio < 0 || callToVideo < 0) {
            Ln.w("Reporting Negative Metric!");
            Ln.w("Negative Metrics Data: startTime=%d, audioStartTime=%d, videoStartTime=%d, callToAudio=%d, callToVideo=%d, callToAV=%d", startTime, audioStart, videoStart, callToAudio, callToVideo, callToAV);
        }

        reportValue(CALL_END_TAG, new CallEndMetricValue(locusId, !inboundCall, callToAudio, callToVideo, callDuration, deviceUrl, requestTimestamp,
                participantID, isGroupMeetup, isNonStandard, isZtm, isSimulcast, isFilmstrip, containsNonUser, usedTcpFallback, cameraFailed, packetStatistics, networkType, gson, mediaStatistics,
                isPaired, resourceType, resourceId, callInitiationOrigin, joinLocusTrackingID, wmeVersion, endReason));

        return this;
    }

    public MetricsBuilder addCallRequest(String locusId, Uri deviceUri, Date locusTimestamp, String participantId) {
        String locusTimestampString = DateUtils.formatUTCDateString(locusTimestamp);
        CallRequestMetricValue value = new CallRequestMetricValue(locusId, deviceUri, locusTimestampString, participantId);
        reportValue(CALL_REQUEST_TAG, value);

        return this;
    }


    public MetricsBuilder addCallAlert(String locusId, Uri deviceUri, String requestTimestamp, String locusTimestamp,
                                       boolean isCaller, boolean isGroup, boolean isNonStandard, String source) {
        CallAlertMetricValue value = new CallAlertMetricValue(locusId, deviceUri, requestTimestamp, locusTimestamp,
                isCaller, isGroup, isNonStandard, source);
        reportValue(CALL_ALERT_TAG, value);

        return this;
    }

    public MetricsBuilder addCallCancelled(String locusId, Uri deviceUri, Date requestTimestamp,
                                           boolean isGroup, boolean isNonStandard, boolean isSimulcast, boolean isFilmstrip,
                                           boolean iceFailed, boolean cameraFailed, String resourceType, String resourceId, String wmeVersion, Gson gson, String networkType,
                                           boolean usedTcpFallback, boolean containsNonUser, String joinLocusTrackingID,
                                           boolean isInbound, CallInitiationOrigin callInitiationOrigin, CallEndReason endReason) {

        boolean isPaired = (!TextUtils.isEmpty(resourceId) && !TextUtils.isEmpty(resourceType));

        addCallMetrics(locusId, 0, null, null, isInbound, isGroup, usedTcpFallback, cameraFailed, containsNonUser, isNonStandard, false, isPaired, isSimulcast, isFilmstrip,
                requestTimestamp, 0, null, null, null, networkType, resourceType, resourceId, gson, deviceUri,
                callInitiationOrigin, joinLocusTrackingID, wmeVersion, endReason);

        return this;
    }

    public MetricsBuilder addCallStunTrace(String locusId, Uri deviceUri, String requestTimestamp, String locusTimestamp,
                                           String detail, Gson gson) {
        CallStunTraceMetricValue value = new CallStunTraceMetricValue(locusId, deviceUri, requestTimestamp, locusTimestamp, detail, gson);
        reportValue(CALL_STUN_TRACE_TAG, value);
        return this;
    }


    public MetricsBuilder addCallNumericDialPrevented() {
        CallNumericDialPrevented value = new CallNumericDialPrevented();
        reportValue(CALL_NUMERIC_DIAL_PREVENTED, value);
        return this;
    }

    public MetricsBuilder addAudioControl(Uri deviceUrl, long durationTime, String roomId, String tag) {
        AudioControlMetricValue value = new AudioControlMetricValue(deviceUrl, durationTime, roomId);
        reportValue(tag, value);
        return this;
    }


    public MetricsBuilder addCallToggleAudioOnly(String tag, String locusId, Uri deviceUri, String requestTimestamp,
                                                    String locusTimestamp, CallInitiationOrigin callInitiationOrigin,
                                                    long callDurationMillis, String networkType) {
        CallToggleAudioOnlyMetricValue value = new CallToggleAudioOnlyMetricValue(locusId, deviceUri, requestTimestamp,
                locusTimestamp, callInitiationOrigin, callDurationMillis, networkType);
        reportValue(tag, value);
        return this;
    }

}
