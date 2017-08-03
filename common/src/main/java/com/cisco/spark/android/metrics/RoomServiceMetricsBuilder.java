package com.cisco.spark.android.metrics;

import com.cisco.spark.android.metrics.value.AltoEnterRoomValue;
import com.cisco.spark.android.metrics.value.AltoMetricsAppToForegroundUnpairedToPairedFlowValue;
import com.cisco.spark.android.metrics.value.AltoMetricsUltrasonicRangeUnpairedToPairedFlowValue;
import com.cisco.spark.android.metrics.value.TokenDecodedUnpairedToPairedFlowValue;
import com.cisco.spark.android.metrics.value.TokenRefreshValue;
import com.cisco.spark.android.metrics.value.UltrasoundTokenValue;

public class RoomServiceMetricsBuilder extends SplunkMetricsBuilder {

    /**
     * More flows and other metrics are coming
     */
    private static final String APP_TO_FG_TO_PAIRED_FLOW_TAG = "appForegroundUnpairedToPairedFlowTiming";
    private static final String DECODED_TO_PAIRED_FLOW_TAG = "ultrasoundDecodedUnpairedToPairedFlowTiming";
    private static final String ULTRASONIC_ENERGY_TO_PAIRED_FLOW_TAG = "ultrasoundRangeUnpairedToPairedFlowTiming";

    private static final String ULTRASOUND_TOKEN_TAG = "ultrasoundToken";
    private static final String TOKEN_REFRESH_TIME_TAG = "altoTokenRefreshTime";
    private static final String RE_ENTERED_ROOM_TAG = "altoReEnteredRoom";
    private static final String ENTERED_ROOM_TAG = "altoEnteredRoom";

    private static final String ANDROID_ULTRASONIC_PAIRING_ERROR = "androidUltrasonicPairingError";
    private static final String ANDROID_AUDIO_RECORDING_ERROR = "androidAudioRecordingError";

    public RoomServiceMetricsBuilder(MetricsEnvironment environment) {
        super(environment);
    }

    public MetricsBuilder reportUltrasoundToken(int errorCorrectionCount) {
        reportValue(ULTRASOUND_TOKEN_TAG, new UltrasoundTokenValue(errorCorrectionCount));
        return this;
    }

    public MetricsBuilder addTokenDecodedUnpairedToPairedFlow(int decodedAnnounced, int announcedRoomevent, int roomeventGotroomstate, String roomUser, String roomUrl) {
        TokenDecodedUnpairedToPairedFlowValue value = new TokenDecodedUnpairedToPairedFlowValue(decodedAnnounced, announcedRoomevent, roomeventGotroomstate, roomUser, roomUrl);
        reportValue(DECODED_TO_PAIRED_FLOW_TAG, value);
        return this;
    }

    public MetricsBuilder addAppForegroundToPairedFlow(int foregroundListen, int listenDecoded, int decodedAnnounced, int announcedRoomevent, int roomeventGotroomstate, int totalPairing, String roomUser, String roomUrl) {
        AltoMetricsAppToForegroundUnpairedToPairedFlowValue value =
                new AltoMetricsAppToForegroundUnpairedToPairedFlowValue(
                        foregroundListen,
                        listenDecoded,
                        decodedAnnounced,
                        announcedRoomevent,
                        roomeventGotroomstate,
                        totalPairing,
                        roomUser,
                        roomUrl);
        reportValue(APP_TO_FG_TO_PAIRED_FLOW_TAG, value);
        return this;
    }

    public MetricsBuilder addUltrasonicRangeToPairedFlow(int inRangeToDecoded, int decodedAnnounced, int announcedRoomevent, int roomeventGotroomstate, int totalPairing, String roomUser, String roomUrl) {
        AltoMetricsUltrasonicRangeUnpairedToPairedFlowValue value =
                new AltoMetricsUltrasonicRangeUnpairedToPairedFlowValue(
                        inRangeToDecoded,
                        decodedAnnounced,
                        announcedRoomevent,
                        roomeventGotroomstate,
                        totalPairing,
                        roomUser,
                        roomUrl);
        reportValue(ULTRASONIC_ENERGY_TO_PAIRED_FLOW_TAG, value);
        return this;
    }

    public MetricsBuilder reportTokenRefresh(long refreshTimeMillis, String roomUser, String roomUrl) {
        reportValue(TOKEN_REFRESH_TIME_TAG, new TokenRefreshValue(refreshTimeMillis, roomUser, roomUrl));
        return this;
    }

    public MetricsBuilder reportEnteredRoom(String roomUser, String roomUrl) {
        reportValue(ENTERED_ROOM_TAG, new AltoEnterRoomValue(roomUser, roomUrl));
        return this;
    }

    public MetricsBuilder reportReEnteredRoom(String roomUser, String roomUrl) {
        reportValue(RE_ENTERED_ROOM_TAG, new AltoEnterRoomValue(roomUser, roomUrl));
        return this;
    }

    public MetricsBuilder reportFailedUltrasonicPairing(String errorMessage) {
        reportValue(ANDROID_ULTRASONIC_PAIRING_ERROR, errorMessage);
        return this;
    }

    public MetricsBuilder reportFailedAudioRecording(String errorMessage) {
        reportValue(ANDROID_AUDIO_RECORDING_ERROR, errorMessage);
        return this;
    }
}
