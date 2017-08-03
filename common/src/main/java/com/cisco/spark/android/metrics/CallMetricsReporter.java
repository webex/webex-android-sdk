package com.cisco.spark.android.metrics;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.callcontrol.model.Call;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.metrics.model.CallAlertSourceType;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.room.RoomService;
import com.cisco.spark.android.room.model.RoomState;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.util.TestUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.cisco.spark.android.metrics.CallMetricsBuilder.CALL_SWITCH_TO_AUDIO_ONLY;
import static com.cisco.spark.android.metrics.CallMetricsBuilder.CALL_SWITCH_TO_VIDEO;

@Singleton
public class CallMetricsReporter {
    private final DeviceRegistration deviceRegistration;
    private final MetricsReporter metricsReporter;
    private final MediaEngine mediaEngine;
    private final Settings settings;
    private final LocusDataCache locusDataCache;
    private final Gson gson;
    private final ConnectivityManager connectivityManager;
    private final RoomService roomService;
    private final SegmentService segmentService;
    private MediaSession mediaSession;

    @Inject
    public CallMetricsReporter(DeviceRegistration deviceRegistration, MetricsReporter metricsReporter, MediaEngine mediaEngine,
                               Settings settings, LocusDataCache locusDataCache,
                               Gson gson, ConnectivityManager connectivityManager,
                               RoomService roomService, SegmentService segmentService) {
        this.deviceRegistration = deviceRegistration;
        this.metricsReporter = metricsReporter;
        this.mediaEngine = mediaEngine;
        this.settings = settings;
        this.locusDataCache = locusDataCache;
        this.gson = gson;
        this.connectivityManager = connectivityManager;
        this.roomService = roomService;
        this.segmentService = segmentService;
    }

    public void setMediaSession(MediaSession mediaSession) {
        this.mediaSession = mediaSession;
    }

    public void reportCallAlertMetrics(LocusKey locusKey, String locusTimestamp, CallAlertSourceType source, boolean isBridge) {
        Ln.i("reportAlertMetrics");

        // don't report metrics if custom call related settings are enabled
        if (isCustomCallSettingEnabled()) {
            return;
        }

        Uri deviceUrl = null;
        if (deviceRegistration.getUrl() != null) {
            deviceUrl = deviceRegistration.getUrl();
        } else {
            Ln.i("deviceRegistration.getUrl() is null.");
        }

        String requestTimestamp = DateUtils.formatUTCDateString(new Date());
        CallMetricsBuilder metricsBuilder = metricsReporter.newCallMetricsBuilder();
        MetricsReportRequest request = metricsBuilder
                .addCallAlert(locusKey.getLocusId(), deviceUrl,
                        requestTimestamp, locusTimestamp, false,
                        isBridge, false, source.toString()).build();
        metricsReporter.enqueueMetricsReport(request);
    }


    public void reportCallCancelledMetrics(Call call) {
        Ln.i("reportCallCancelledMetrics");

        if (call == null || call.getLocusData() == null)
            return;

        // don't report metrics if custom call related settings are enabled
        if (isCustomCallSettingEnabled()) {
            return;
        }

        boolean iceFailed = false;
        boolean cameraFailed = false;
        boolean isSimulcast = false;
        boolean usedTcpFallback = false;
        if (mediaSession != null) {
            iceFailed = mediaSession.iceFailed();
            cameraFailed = mediaSession.cameraFailed();
            isSimulcast = mediaSession.simulcastEnabled();
            usedTcpFallback = mediaSession.usedTcpFallback();
        }


        LocusData locusData = call.getLocusData();
        boolean isFilmstrip = deviceRegistration.getFeatures().isMultistreamEnabled();
        boolean containsNonUsers = locusData.containsNonUsers();
        boolean isInbound = !locusData.getLocus().getSelf().isCreator();

        String resourceType = null;
        String resourceId = null;
        if (roomService.getCallController().isCallingUsingRoom()) {
            RoomState roomState = roomService.getRoomState();
            if (roomState != null && roomState.getRoomIdentity() != null) {
                resourceType = roomState.getRoomType();
                resourceId = roomState.getRoomIdentity().toString();
            }
        }

        String requestTimestamp = DateUtils.formatUTCDateString(new Date());
        CallMetricsBuilder metricsBuilder = metricsReporter.newCallMetricsBuilder();
        MetricsReportRequest request = metricsBuilder
                .addCallCancelled(call.getLocusKey().getLocusId(), deviceRegistration.getUrl(), locusData.getStartTime(),
                        locusData.isBridge(), false, isSimulcast, isFilmstrip, iceFailed, cameraFailed,
                        resourceType, resourceId, getPrettyWmeVersion(), gson,
                        getNetworkType(), usedTcpFallback, containsNonUsers, call.getJoinLocusTrackingID(), isInbound,
                        call.getCallInitiationOrigin(), call.getCallEndReason(), segmentService
                ).build();
        metricsReporter.enqueueMetricsReport(request);
    }

    public void reportJoinMetrics(Call call) {
        Ln.i("reportJoinMetrics");

        if (call == null || call.getLocusData() == null)
            return;

        // don't report metrics if custom call related settings are enabled
        if (isCustomCallSettingEnabled()) {
            return;
        }


        CallMetricsBuilder metricsBuilder;
        LocusData locusData = call.getLocusData();
        if (isTestUserInCall(locusData.getLocus())) {
            metricsBuilder = metricsReporter.newCallMetricsBuilder(MetricsEnvironment.ENV_TEST);
        } else {
            metricsBuilder = metricsReporter.newCallMetricsBuilder();
        }

        MetricsReportRequest request = metricsBuilder.
                addCallRequest(locusData.getKey().getLocusId(), deviceRegistration.getUrl(),
                        locusData.getLocus().getFullState().getLastActive(),
                        locusData.getLocus().getSelf().getId().toString()).build();

        metricsReporter.enqueueMetricsReport(request);
    }

    public void reportLeaveMetrics(Call call) {
        Ln.i("reportLeaveMetrics");
        Ln.i("MetricsReporter: Begin Metrics Report");

        if (call == null || call.getLocusData() == null) {
            Ln.i("reportLeaveMetrics, no call info");
            return;
        }

        boolean isNonStandard = isCustomCallSettingEnabled();

        long startTime = call.getEpochStartTime();

        Date audioStart = null;
        Date videoStart = null;
        boolean usedTcpFallback = false;
        boolean cameraFailed = false;
        boolean isSimulcast = false;
        String sessionStats = "";
        String packetStats = "";

        if (mediaSession != null) {
            audioStart = mediaSession.getFirstAudioPacketReceivedTime();
            videoStart = mediaSession.getFirstVideoPacketReceivedTime();

            usedTcpFallback = mediaSession.usedTcpFallback();
            cameraFailed = mediaSession.cameraFailed();
            isSimulcast = mediaSession.simulcastEnabled();

            sessionStats = mediaSession.getSessionStats();
            packetStats = mediaSession.getPacketStats();
        }


        LocusData locusData = call.getLocusData();
        boolean isInbound = !locusData.getLocus().getSelf().isCreator();
        String participantId = locusData.getLocus().getSelf().getId().toString();
        Uri deviceUrl = null;
        if (deviceRegistration.getUrl() != null) {
            deviceUrl = deviceRegistration.getUrl();
        } else {
            Ln.i("deviceRegistration.getUrl() is null.");
        }

        boolean isPaired = roomService.getCallController().isCallingUsingRoom();
        String resourceType = null;
        String resourceId = null;
        if (isPaired) {
            RoomState roomState = roomService.getRoomState();
            if (roomState != null && roomState.getRoomIdentity() != null) {
                resourceType = roomState.getRoomType();
                resourceId = roomState.getRoomIdentity().toString();
            }
        }

        CallMetricsBuilder metricsBuilder = metricsReporter.newCallMetricsBuilder();

        boolean containsNonUsers = locusData.containsNonUsers();
        boolean testUserInCall = isTestUserInCall(locusData.getLocus());
        if (testUserInCall) {
            Ln.i("reportLeaveMetrics, test user on call");
            isNonStandard = true;
        }

        boolean isFilmstrip = deviceRegistration.getFeatures().isMultistreamEnabled();

        MetricsReportRequest request = metricsBuilder
                .addCallMetrics(call.getLocusKey().getLocusId(), startTime, audioStart,
                        videoStart, isInbound, locusData.isBridge(), usedTcpFallback, cameraFailed,
                        containsNonUsers, isNonStandard, locusDataCache.isZtmCall(call.getLocusKey()), isPaired,
                        isSimulcast, isFilmstrip, locusData.getLocus().getFullState().getLastActive(), locusData.getLocus().getParticipants().size(),
                        participantId, sessionStats, packetStats,
                        getNetworkType(), resourceType, resourceId,
                        gson, deviceUrl, call.getCallInitiationOrigin(), call.getJoinLocusTrackingID(), getPrettyWmeVersion(),
                        call.getCallEndReason(), segmentService
                ).build();
        metricsReporter.enqueueMetricsReport(request);
        Ln.i("= Call Metrics Report =" + gson.toJson(request.getMetrics().get(0).getValue()) + "= End Metrics Report =");

        try {
            JSONObject callStats = new JSONObject(gson.toJson(request.getMetrics().get(0).getValue()));
            JSONObject mediaStats = callStats.getJSONObject("mediaStatistics");
            callStats.remove("mediaStatistics");
            Ln.i("= ABC Call Metrics Report =" + callStats.toString() + "= End ABC Call Metrics Report =");
            Ln.i("= ABC Call mediaStatistics Report=" + mediaStats.toString() + "= End ABC Call mediaStatistics Report =");
        } catch (JSONException e) {
            Ln.i("Failed to parse JSON for ABC report");
        }
    }


    public void reportCallStunTraceMetrics(Call call, String detail) {
        Ln.i("reportCallStunTraceMetrics");

        if (call == null || call.getLocusData() == null) {
            return;
        }

        // don't report metrics if custom call related settings are enabled
        if (isCustomCallSettingEnabled()) {
            return;
        }

        String requestTimestamp = DateUtils.formatUTCDateString(new Date());
        CallMetricsBuilder metricsBuilder = metricsReporter.newCallMetricsBuilder();
        MetricsReportRequest request = metricsBuilder
                .addCallStunTrace(call.getLocusKey().getLocusId(), deviceRegistration.getUrl(),
                        requestTimestamp, DateUtils.formatUTCDateString(call.getLocusData().getStartTime()), detail, gson).build();
        metricsReporter.enqueueMetricsReport(request);
    }


    public void reportCallNumericDialPrevented() {
        Ln.i("reportCallNumericDialPrevented");

        // don't report metrics if custom call related settings are enabled
        if (isCustomCallSettingEnabled()) {
            return;
        }

        CallMetricsBuilder metricsBuilder = metricsReporter.newCallMetricsBuilder();
        MetricsReportRequest request = metricsBuilder.addCallNumericDialPrevented().build();
        metricsReporter.enqueueMetricsReport(request);
    }


    public void reportCallAudioToggleMetrics(Call call) {
        Ln.i("reportCallAudioToggleMetrics, isAudioCall = " + call.isAudioCall());

        // don't report metrics if custom call related settings are enabled
        if (isCustomCallSettingEnabled()) {
            return;
        }

        long startTime = call.getEpochStartTime();
        long callDuration = System.currentTimeMillis() - startTime;

        String tag;
        if (call.isAudioCall()) {
            tag = CALL_SWITCH_TO_AUDIO_ONLY;
        } else {
            tag = CALL_SWITCH_TO_VIDEO;
        }

        String requestTimestamp = DateUtils.formatUTCDateString(new Date());
        CallMetricsBuilder metricsBuilder = metricsReporter.newCallMetricsBuilder();
        MetricsReportRequest request = metricsBuilder
                .addCallToggleAudioOnly(tag, call.getLocusKey().getLocusId(), deviceRegistration.getUrl(),
                        requestTimestamp, DateUtils.formatUTCDateString(call.getLocusData().getStartTime()),
                        call.getCallInitiationOrigin(), callDuration, getNetworkType())
                .build();
        metricsReporter.enqueueMetricsReport(request);
    }


    public void reportMuteControlMetrics(String roomId) {
        Ln.i("reportMuteControlMetrics");
        reportAudioControlMetrics(Long.MIN_VALUE, roomId, CallMetricsBuilder.MUTE_CONTROL);
    }

    public void reportVolumeControlMetrics(String roomId) {
        Ln.i("reportVolumeControlMetrics");
        reportAudioControlMetrics(Long.MIN_VALUE, roomId, CallMetricsBuilder.VOLUME_CONTROL);
    }

    public void reportMuteActionMetrics(long durationTime, String roomId) {
        Ln.i("reportMuteActionMetrics");
        reportAudioControlMetrics(durationTime, roomId, CallMetricsBuilder.MUTE_ACTION);
    }

    public void reportUnMuteActionMetrics(long durationTime, String roomId) {
        Ln.i("reportUnMuteActionMetrics");
        reportAudioControlMetrics(durationTime, roomId, CallMetricsBuilder.UNMUTE_ACTION);
    }

    public void reportVolumeActionMetrics(long durationTime, String roomId) {
        Ln.i("reportVolumeActionMetrics");
        reportAudioControlMetrics(durationTime, roomId, CallMetricsBuilder.VOLUME_ACTION);
    }

    public void reportJoinLocusMetrics(LocusKey locusKey, String usingResource, String result, String errorMessage) {
        Ln.i("reportJoinLocusMetrics");
        CallMetricsBuilder metricsBuilder;
        metricsBuilder = metricsReporter.newCallMetricsBuilder();
        MetricsReportRequest request = metricsBuilder.
                addJoinLocus(locusKey == null ? null : locusKey.getLocusId(), deviceRegistration.getUrl(), usingResource, result, errorMessage).build();
        metricsReporter.enqueueMetricsReport(request);
    }

    private boolean isTestUserInCall(Locus locus) {
        // check if any of the participants are test users
        Ln.i("isTestUserInCall, number participants = " + locus.getParticipants().size());
        for (LocusParticipant participant : locus.getParticipants()) {
            if (participant.getPerson() != null && participant.getPerson().getEmail() != null && TestUtils.isTestUser(participant.getPerson().getEmail())) {
                // protect privacy by not logging email or names
                Ln.i("isTestUserInCall() returning true for: %s", participant.getPerson().getId());
                return true;
            }
        }
        Ln.i("isTestUserInCall() returning false");
        return false;
    }

    private boolean isCustomCallSettingEnabled() {
        boolean customCallSettingsEanbled = settings.getMediaOverrideIpAddress().length() > 0 || settings.getLinusName().length() > 0
                || !"default".equalsIgnoreCase(settings.getAudioCodec())
                || settings.getCustomWdmUrl().length() > 0 || settings.getCustomCallFeature().length() > 0;


        if (customCallSettingsEanbled) {
            Ln.i("Custom Settings Enabled: mediaOverrideIpAddress=%s, linusName=%s, audioCodec=%s, customWdm=%s, customCallFeature=%s",
                    settings.getMediaOverrideIpAddress(), settings.getLinusName(), settings.getAudioCodec(),
                    settings.getCustomWdmUrl(), settings.getCustomCallFeature());
        } else {
            Ln.i("No custom call settings enabled");
        }

        return customCallSettingsEanbled;
    }

    private String getNetworkType() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.getTypeName() != null) ? networkInfo.getTypeName().toUpperCase(Locale.getDefault()) : "";
    }

    private String getPrettyWmeVersion() {
        String value = mediaEngine.getVersion(); // Format: "AABBCCDD (MediaSession)"
        if (value.length() > 8 && value.substring(0, 8).matches("[0-9]+")) {
            String major = value.substring(0, 2);
            String minor = value.substring(2, 4);
            String patch = value.substring(4, 6);
            String build = value.substring(6, 8);
            if (!TextUtils.isEmpty(major) && !TextUtils.isEmpty(minor) && !TextUtils.isEmpty(patch))
                value = Integer.valueOf(major).toString() + "." + Integer.valueOf(minor).toString() + "." + Integer.valueOf(patch).toString();
            if (!TextUtils.isEmpty(build) && !"00".equalsIgnoreCase(build))
                value += "." + Integer.valueOf(build).toString();
        }
        return value;
    }

    private void reportAudioControlMetrics(long durationTime, String roomId, String tag) {
        CallMetricsBuilder metricsBuilder;
        metricsBuilder = metricsReporter.newCallMetricsBuilder();
        MetricsReportRequest request = metricsBuilder.
                addAudioControl(deviceRegistration.getUrl(), durationTime, roomId, tag).build();

        metricsReporter.enqueueMetricsReport(request);
    }
}
