package com.ciscowebex.androidsdk.internal.metric;

import android.support.annotation.Nullable;
import com.cisco.wx2.diagnostic_events.ClientEvent;
import com.cisco.wx2.diagnostic_events.MediaLine;
import com.ciscowebex.androidsdk.internal.ErrorDetail;
import com.ciscowebex.androidsdk.internal.media.WMEngine;
import com.ciscowebex.androidsdk.internal.model.GenericMetricModel;
import com.ciscowebex.androidsdk.internal.model.LocusKeyModel;
import com.ciscowebex.androidsdk.phone.internal.CallImpl;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.github.benoitdion.ln.Ln;

import java.util.List;

/*
 * An implementation of https://sqbu-github.cisco.com/WebExSquared/event-dictionary
 * See https://sqbu-github.cisco.com/WebExSquared/event-dictionary/wiki/Event-Definitions for more
 * details
 */
public class CallAnalyzerReporter {

    public static final CallAnalyzerReporter shared = new CallAnalyzerReporter();

    private PhoneImpl phone;

    private MetricService service;

    private CallAnalyzerReporter() {
    }

    public void init(PhoneImpl phone) {
        this.phone = phone;
        this.service = new MetricService(phone);
    }

    private ClientEventBuilder getClientEventBuilder() {
        return new ClientEventBuilder(phone);
    }

    private MediaQualityBuilder getMediaQualityEventBuilder() {
        return new MediaQualityBuilder(phone);
    }

    private void postMetric(GenericMetricModel metric) {
        if (metric != null) {
            service.post(metric);
        }
    }

    public void reportClientStartedFromCrash() {
        Ln.i("reportClientStartedFromCrash");
        // If we can pass in info on the last call we were in that would be a bonus
        postMetric(getClientEventBuilder().clientCrash().build());
    }

    public void reportMediaEngineCrash() {
        Ln.i("reportMediaEngineCrash");
        // If we can pass in info on the last call we were in that would be a bonus
        postMetric(getClientEventBuilder().mediaEngineCrash().build());
    }

    public void reportCallInitiated(CallImpl call) {
        Ln.i("reportCallInitiated");
        // This SHOULD be reported from the UI the moment you hit the button to initiate a call,
        // but for now I'm putting it in CCS
        postMetric(getClientEventBuilder().addCall(call).callInitiated(call).build());
    }

    // Should be called by: SDK
    public void reportLocalSdpGenerated(CallImpl call) {
        Ln.i("reportLocalSdpGenerated");
        postMetric(getClientEventBuilder().addCall(call).localSdpGenerated().build());
    }

    public void reportLocalSdpGenereatedError(CallImpl call, long errorCode, String description) {
        Ln.i("reportLocalSdpGeneratedError");
        postMetric(getClientEventBuilder().addCall(call).localSdpGeneratedFailed(errorCode, description).build());
    }

    // Should be called by: SDK
    public void reportJoinRequest(String callId, LocusKeyModel locusKey) {
        Ln.i("reportJoinRequest");
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinRequest().addReachabilityStatus(phone.getReachability()).build());
    }

    // Should be called by: SDK
    public void reportJoinResponseSuccess(String callId, LocusKeyModel locusKey) {
        Ln.i("reportJoinResponseSuccess");
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinResponse(true).build());
    }

    // Should be called by: SDK
    public void reportJoinResponseError(String callId, LocusKeyModel locusKey, int httpCode, @Nullable ErrorDetail detail) {
        Ln.i("reportJoinResponseError");
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinResponse(false).addLocusErrorResponse(true, httpCode, detail).build());
    }

    // Should be called by: SDK
    public void reportJoinResponseError(String callId, LocusKeyModel locusKey, String desc) {
        Ln.i("reportJoinResponseError, network issue");
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinResponse(false).addLocusErrorResponse(true, desc).build());
    }

    // Should be called by: SDK
    public void reportMediaCapabilities(CallImpl call) {
        reportMediaCapabilities(call, 0);
    }

    public void reportMediaCapabilities(CallImpl call, int errorCode) {
        Ln.i("reportMediaCapabilities");
        if (call == null || call.getMedia() == null) {
            Ln.w("Unable to get Media Capabilities.");
            return;
        }
        postMetric(getClientEventBuilder().addCall(call).mediaCapabilities().addMediaCapabilities(call.getMedia().getCapability()).addMediaDeviceError(errorCode).build());
    }

    // Should be called by: SDK
    public void reportFloorGrantRequest(String correlationId, LocusKeyModel locusKey) {
        Ln.i("reportFloorGrantRequest");
        postMetric(getClientEventBuilder().addCall(correlationId, locusKey).floorGrantRequest().build());
    }

    // Should be called by: SDK
    public void reportFloorGrantedLocal(CallImpl call) {
        Ln.i("reportFloorGrantedLocal");
        postMetric(getClientEventBuilder().addCall(call).floorGrantedLocal().build());
    }

    // Should be called by: SDK
    public void reportMediaEngineReady(CallImpl call) {
        Ln.i("reportMediaEngineReady");
        postMetric(getClientEventBuilder().addCall(call).mediaEngineReady().build());
    }

    // Should be called by: SDK
    public void reportRemoteSdpReceived(CallImpl call) {
        Ln.i("reportRemoteSdpReceived");
        postMetric(getClientEventBuilder().addCall(call).remoteSDPRx().build());
    }

    // Should be called by: SDK
    public void reportClientNotificationReceived(LocusKeyModel locusKey, boolean mercury) {
        Ln.i("reportClientNotificationReceived");
        postMetric(getClientEventBuilder().addCall(null, locusKey).addTrigger(mercury ? ClientEvent.Trigger.MERCURY_EVENT : ClientEvent.Trigger.OTHER).notificationReceived().build());
    }

    // Should be call by: SDK
    public void reportCallDeclined(CallImpl call) {
        Ln.i("reportCallDeclined");
        postMetric(getClientEventBuilder().addCall(call).callDeclined().build());
    }

    // Should be called by: SDK
    public void reportIceStart(CallImpl call) {
        Ln.i("reportIceStart");
        postMetric(getClientEventBuilder().addCall(call).iceStart().build());
    }

    // Should be called by: SDK
    public void reportIceEnd(CallImpl call, boolean success, List<MediaLine> iceMediaLineList) {
        Ln.i("reportIceEnd");
        postMetric(getClientEventBuilder().addCall(call).iceEnd(success, iceMediaLineList).build());
    }

    // Should be called by: SDK
    public void reportMediaRxStart(CallImpl call, WMEngine.Media mediaType, Long csi) {
        Ln.i("reportMediaRxStart: mediaType=%s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).rxMediaStart(mediaType, csi).build());
    }

    // Should be called by: SDK
    // Need to clarify if this is whole session stopped, or just a transient stop (i.e. something like WME video blocked)
    public void reportMediaRxStop(CallImpl call, WMEngine.Media mediaType, Integer mediaStatus) {
        Ln.i("reportMediaRxStop: mediaType=%s, mediaStatus=%d", mediaType, mediaStatus);
        postMetric(getClientEventBuilder().addCall(call).rxMediaStop(mediaType, mediaStatus).build());
    }

    // Should be called by: SDK
    public void reportMediaTxStart(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMediaTxStart: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).txMediaStart(mediaType).build());
    }

    // Should be called by: SDK
    // Need to clarify if this is whole session stopped, or just a transient stop  (i.e. something like WME video blocked)
    public void reportMediaTxStop(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMediaTxStop: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).txMediaStop(mediaType).build());
    }

    // Should be called by: Client UI
    public void reportMediaRenderStart(CallImpl call, WMEngine.Media mediaType, Long csi) {
        Ln.i("reportMediaRenderStart: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).mediaRenderStart(mediaType, csi).build());
    }

    // Should be called by: Client UI
    public void reportMediaRenderStop(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMediaRenderStop: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).mediaRenderStop(mediaType).build());
    }

    public void reportShareCsiChanged(CallImpl call, Long csi) {
        Ln.i("reportShareCsiChanged");
        postMetric(getClientEventBuilder().addCall(call).shareCsiChanged(csi).build());
    }

    // Should be called by: SDK
    public void reportMediaReconnecting(CallImpl call) {
        Ln.i("reportMediaReconnecting");
        postMetric(getClientEventBuilder().addCall(call).mediaReconnecting().build());
    }

    // Should be called by: SDK
    public void reportMediaRecovered(CallImpl call, boolean newSession) {
        Ln.i("reportMediaRecovered");
        postMetric(getClientEventBuilder().addCall(call).mediaRecovered(newSession).build());
    }

    // Should be called by: SDK
    public void reportShareInitiated(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportShareInitiated");
        postMetric(getClientEventBuilder().addCall(call).shareInitiated(mediaType).build());
    }

    // Should be called by: SDK
    public void reportShareStopped(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportShareStopped");
        postMetric(getClientEventBuilder().addCall(call).shareStopped(mediaType).build());
    }

    // Should be called by: Client UI
    public void reportShareSelectedApp(CallImpl call) {
        Ln.i("reportShareSelectedApp");
        postMetric(getClientEventBuilder().addCall(call).shareAppSelected().build());
    }

    // Should be called by: Client
    public void reportShareLayoutDisplayed(CallImpl call) {
        Ln.i("reportShareLayoutDisplayed");
        postMetric(getClientEventBuilder().addCall(call).shareDisplayed().build());
    }

    // Should be called by: SDK
    public void reportMuted(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMuted");
        postMetric(getClientEventBuilder().addCall(call).muted(mediaType).build());
    }

    // Should be called by: SDK
    public void reportUnmuted(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportUnmuted");
        postMetric(getClientEventBuilder().addCall(call).unmuted(mediaType).build());
    }

    // Should be called by: SDK
    public void reportCallLeave(CallImpl call) {
        Ln.i("reportCallLeave");
        postMetric(getClientEventBuilder().addCall(call).callLeft(true).build());
    }

    // SCA/SRC reports - presumably they will want details of what was in the SCA/SCR?
    // is there a standard format?

    // Should be called by: SDK
    public void reportMultistreamScaTx(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMultistreamScaTx");
        postMetric(getClientEventBuilder().addCall(call).scaTx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScaRx(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMultistreamScaRx");
        postMetric(getClientEventBuilder().addCall(call).scaRx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScrTx(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMultistreamScrTx");
        postMetric(getClientEventBuilder().addCall(call).scrTx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScrRx(CallImpl call, WMEngine.Media mediaType) {
        Ln.i("reportMultistreamScrRx");
        postMetric(getClientEventBuilder().addCall(call).scrRx(mediaType).build());
    }

    public void reportNetworkChanged(CallImpl call) {
        Ln.i("reportNetworkChanged");
        postMetric(getClientEventBuilder().addCall(call).callNetworkChanged().build());
    }

    public void reportAppEnteringBackground(CallImpl call) {
        Ln.i("reportAppEnteringBackground");
        postMetric(getClientEventBuilder().addCall(call).callAppEnteringBackground().build());
    }

    public void reportAppEnteringForeground(CallImpl call) {
        Ln.i("reportAppEnteringForeground");
        postMetric(getClientEventBuilder().addCall(call).callAppEnteringForeground().build());
    }

    // --------------- CMR related diagnostics - usage TBA in future, so ignore for now ------------

    // Should be called by: Client?
    public void reportPinPromptShown(CallImpl call) {
        Ln.i("reportPinPromptShown");
        // So this should probably come from the UI?
        postMetric(getClientEventBuilder().addCall(call).pinPromptShown().build());
    }

    // Should be called by: SDK?
    public void reportPinEntered(CallImpl call) {
        Ln.i("reportPinEntered");
        postMetric(getClientEventBuilder().addCall(call).pinEntered().build());
    }

    // Should be called by: SDK or Client?
    public void reportMeetingLobbyEntered(CallImpl call) {
        Ln.i("reportMeetingLobbyEntered");
        // AMBIGUITY: Is this just the "waiting for host" screen, or is it the whole PIN entry +
        // "waiting for host" process. This is TBA in the future
        postMetric(getClientEventBuilder().addCall(call).lobbyEntered().build());
    }

    // Should be called by: SDK or Client?
    public void reportMeetingLobbyExited(CallImpl call) {
        Ln.i("reportMeetingLobbyExited");
        // AMBIGUITY: Is this just the "waiting for host" screen, or is it the whole PIN entry +
        // "waiting for host" process. This is TBA in the future
        postMetric(getClientEventBuilder().addCall(call).lobbyExited().build());
    }

    // ---------------------------------------------------------------------------------------------
    public void reportMediaQualityMetrics(CallImpl call, String metrics) {
        Ln.i("reportMediaQualityMetrics");
        postMetric(getMediaQualityEventBuilder().addCall(call).setMediaQualityEvent(metrics).build());
    }

    public void reportMercuryConnectionStatus(CallImpl call, boolean connected) {
        Ln.i("reportMercuryConnectionStatus connected=" + connected);
        if (connected) {
            postMetric(getClientEventBuilder().addCall(call).mercuryConnectionRestored().build());
        } else {
            postMetric(getClientEventBuilder().addCall(call).mercuryConnectionLost().build());
        }
    }
}
