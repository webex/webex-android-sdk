/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
        Ln.d("reportClientStartedFromCrash");
        // If we can pass in info on the last call we were in that would be a bonus
        postMetric(getClientEventBuilder().clientCrash().build());
    }

    public void reportMediaEngineCrash() {
        Ln.d("reportMediaEngineCrash");
        // If we can pass in info on the last call we were in that would be a bonus
        postMetric(getClientEventBuilder().mediaEngineCrash().build());
    }

    public void reportCallInitiated(CallImpl call) {
        Ln.d("reportCallInitiated");
        // This SHOULD be reported from the UI the moment you hit the button to initiate a call,
        // but for now I'm putting it in CCS
        postMetric(getClientEventBuilder().addCall(call).callInitiated(call).build());
    }

    // Should be called by: SDK
    public void reportLocalSdpGenerated(CallImpl call) {
        Ln.d("reportLocalSdpGenerated");
        postMetric(getClientEventBuilder().addCall(call).localSdpGenerated().build());
    }

    public void reportLocalSdpGenereatedError(CallImpl call, long errorCode, String description) {
        Ln.d("reportLocalSdpGeneratedError");
        postMetric(getClientEventBuilder().addCall(call).localSdpGeneratedFailed(errorCode, description).build());
    }

    // Should be called by: SDK
    public void reportJoinRequest(String callId, LocusKeyModel locusKey) {
        Ln.d("reportJoinRequest");
        //postMetric(getClientEventBuilder().addCall(callId, locusKey).joinRequest().addReachabilityStatus(phone.getReachability()).build());
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinRequest().build());
    }

    // Should be called by: SDK
    public void reportJoinResponseSuccess(String callId, LocusKeyModel locusKey) {
        Ln.d("reportJoinResponseSuccess");
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinResponse(true).build());
    }

    // Should be called by: SDK
    public void reportJoinResponseError(String callId, LocusKeyModel locusKey, int httpCode, @Nullable ErrorDetail detail) {
        Ln.d("reportJoinResponseError");
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinResponse(false).addLocusErrorResponse(true, httpCode, detail).build());
    }

    // Should be called by: SDK
    public void reportJoinResponseError(String callId, LocusKeyModel locusKey, String desc) {
        Ln.d("reportJoinResponseError, network issue");
        postMetric(getClientEventBuilder().addCall(callId, locusKey).joinResponse(false).addLocusErrorResponse(true, desc).build());
    }

    // Should be called by: SDK
    public void reportMediaCapabilities(CallImpl call) {
        reportMediaCapabilities(call, 0);
    }

    public void reportMediaCapabilities(CallImpl call, int errorCode) {
        Ln.d("reportMediaCapabilities");
        if (call == null || call.getMedia() == null) {
            Ln.w("Unable to get Media Capabilities.");
            return;
        }
        postMetric(getClientEventBuilder().addCall(call).mediaCapabilities().addMediaCapabilities(call.getMedia().getCapability()).addMediaDeviceError(errorCode).build());
    }

    // Should be called by: SDK
    public void reportFloorGrantRequest(String correlationId, LocusKeyModel locusKey) {
        Ln.d("reportFloorGrantRequest");
        postMetric(getClientEventBuilder().addCall(correlationId, locusKey).floorGrantRequest().build());
    }

    // Should be called by: SDK
    public void reportFloorGrantedLocal(CallImpl call) {
        Ln.d("reportFloorGrantedLocal");
        postMetric(getClientEventBuilder().addCall(call).floorGrantedLocal().build());
    }

    // Should be called by: SDK
    public void reportMediaEngineReady(CallImpl call) {
        Ln.d("reportMediaEngineReady");
        postMetric(getClientEventBuilder().addCall(call).mediaEngineReady().build());
    }

    // Should be called by: SDK
    public void reportRemoteSdpReceived(CallImpl call) {
        Ln.d("reportRemoteSdpReceived");
        postMetric(getClientEventBuilder().addCall(call).remoteSDPRx().build());
    }

    // Should be called by: SDK
    public void reportClientNotificationReceived(LocusKeyModel locusKey, boolean mercury) {
        Ln.d("reportClientNotificationReceived");
        postMetric(getClientEventBuilder().addCall(null, locusKey).addTrigger(mercury ? ClientEvent.Trigger.MERCURY_EVENT : ClientEvent.Trigger.OTHER).notificationReceived().build());
    }

    // Should be call by: SDK
    public void reportCallDeclined(CallImpl call) {
        Ln.d("reportCallDeclined");
        postMetric(getClientEventBuilder().addCall(call).callDeclined().build());
    }

    // Should be called by: SDK
    public void reportIceStart(CallImpl call) {
        Ln.d("reportIceStart");
        postMetric(getClientEventBuilder().addCall(call).iceStart().build());
    }

    // Should be called by: SDK
    public void reportIceEnd(CallImpl call, boolean success, List<MediaLine> iceMediaLineList) {
        Ln.d("reportIceEnd");
        postMetric(getClientEventBuilder().addCall(call).iceEnd(success, iceMediaLineList).build());
    }

    // Should be called by: SDK
    public void reportMediaRxStart(CallImpl call, WMEngine.Media mediaType, Long csi) {
        Ln.d("reportMediaRxStart: mediaType=%s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).rxMediaStart(mediaType, csi).build());
    }

    // Should be called by: SDK
    // Need to clarify if this is whole session stopped, or just a transient stop (i.e. something like WME video blocked)
    public void reportMediaRxStop(CallImpl call, WMEngine.Media mediaType, Integer mediaStatus) {
        Ln.d("reportMediaRxStop: mediaType=%s, mediaStatus=%d", mediaType, mediaStatus);
        postMetric(getClientEventBuilder().addCall(call).rxMediaStop(mediaType, mediaStatus).build());
    }

    // Should be called by: SDK
    public void reportMediaTxStart(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMediaTxStart: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).txMediaStart(mediaType).build());
    }

    // Should be called by: SDK
    // Need to clarify if this is whole session stopped, or just a transient stop  (i.e. something like WME video blocked)
    public void reportMediaTxStop(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMediaTxStop: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).txMediaStop(mediaType).build());
    }

    // Should be called by: Client UI
    public void reportMediaRenderStart(CallImpl call, WMEngine.Media mediaType, Long csi) {
        Ln.d("reportMediaRenderStart: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).mediaRenderStart(mediaType, csi).build());
    }

    // Should be called by: Client UI
    public void reportMediaRenderStop(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMediaRenderStop: %s", mediaType);
        postMetric(getClientEventBuilder().addCall(call).mediaRenderStop(mediaType).build());
    }

    public void reportShareCsiChanged(CallImpl call, Long csi) {
        Ln.d("reportShareCsiChanged");
        postMetric(getClientEventBuilder().addCall(call).shareCsiChanged(csi).build());
    }

    // Should be called by: SDK
    public void reportMediaReconnecting(CallImpl call) {
        Ln.d("reportMediaReconnecting");
        postMetric(getClientEventBuilder().addCall(call).mediaReconnecting().build());
    }

    // Should be called by: SDK
    public void reportMediaRecovered(CallImpl call, boolean newSession) {
        Ln.d("reportMediaRecovered");
        postMetric(getClientEventBuilder().addCall(call).mediaRecovered(newSession).build());
    }

    // Should be called by: SDK
    public void reportShareInitiated(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportShareInitiated");
        postMetric(getClientEventBuilder().addCall(call).shareInitiated(mediaType).build());
    }

    // Should be called by: SDK
    public void reportShareStopped(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportShareStopped");
        postMetric(getClientEventBuilder().addCall(call).shareStopped(mediaType).build());
    }

    // Should be called by: Client UI
    public void reportShareSelectedApp(CallImpl call) {
        Ln.d("reportShareSelectedApp");
        postMetric(getClientEventBuilder().addCall(call).shareAppSelected().build());
    }

    // Should be called by: Client
    public void reportShareLayoutDisplayed(CallImpl call) {
        Ln.d("reportShareLayoutDisplayed");
        postMetric(getClientEventBuilder().addCall(call).shareDisplayed().build());
    }

    // Should be called by: SDK
    public void reportMuted(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMuted");
        postMetric(getClientEventBuilder().addCall(call).muted(mediaType).build());
    }

    // Should be called by: SDK
    public void reportUnmuted(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportUnmuted");
        postMetric(getClientEventBuilder().addCall(call).unmuted(mediaType).build());
    }

    // Should be called by: SDK
    public void reportCallLeave(CallImpl call) {
        Ln.d("reportCallLeave");
        postMetric(getClientEventBuilder().addCall(call).callLeft(true).build());
    }

    // SCA/SRC reports - presumably they will want details of what was in the SCA/SCR?
    // is there a standard format?

    // Should be called by: SDK
    public void reportMultistreamScaTx(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMultistreamScaTx");
        postMetric(getClientEventBuilder().addCall(call).scaTx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScaRx(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMultistreamScaRx");
        postMetric(getClientEventBuilder().addCall(call).scaRx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScrTx(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMultistreamScrTx");
        postMetric(getClientEventBuilder().addCall(call).scrTx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScrRx(CallImpl call, WMEngine.Media mediaType) {
        Ln.d("reportMultistreamScrRx");
        postMetric(getClientEventBuilder().addCall(call).scrRx(mediaType).build());
    }

    public void reportNetworkChanged(CallImpl call) {
        Ln.d("reportNetworkChanged");
        postMetric(getClientEventBuilder().addCall(call).callNetworkChanged().build());
    }

    public void reportAppEnteringBackground(CallImpl call) {
        Ln.d("reportAppEnteringBackground");
        postMetric(getClientEventBuilder().addCall(call).callAppEnteringBackground().build());
    }

    public void reportAppEnteringForeground(CallImpl call) {
        Ln.d("reportAppEnteringForeground");
        postMetric(getClientEventBuilder().addCall(call).callAppEnteringForeground().build());
    }

    // --------------- CMR related diagnostics - usage TBA in future, so ignore for now ------------

    // Should be called by: Client?
    public void reportPinPromptShown(CallImpl call) {
        Ln.d("reportPinPromptShown");
        // So this should probably come from the UI?
        postMetric(getClientEventBuilder().addCall(call).pinPromptShown().build());
    }

    // Should be called by: SDK?
    public void reportPinEntered(CallImpl call) {
        Ln.d("reportPinEntered");
        postMetric(getClientEventBuilder().addCall(call).pinEntered().build());
    }

    // Should be called by: SDK or Client?
    public void reportMeetingLobbyEntered(CallImpl call) {
        Ln.d("reportMeetingLobbyEntered");
        // AMBIGUITY: Is this just the "waiting for host" screen, or is it the whole PIN entry +
        // "waiting for host" process. This is TBA in the future
        postMetric(getClientEventBuilder().addCall(call).lobbyEntered().build());
    }

    // Should be called by: SDK or Client?
    public void reportMeetingLobbyExited(CallImpl call) {
        Ln.d("reportMeetingLobbyExited");
        // AMBIGUITY: Is this just the "waiting for host" screen, or is it the whole PIN entry +
        // "waiting for host" process. This is TBA in the future
        postMetric(getClientEventBuilder().addCall(call).lobbyExited().build());
    }

    // ---------------------------------------------------------------------------------------------
    public void reportMediaQualityMetrics(CallImpl call, String metrics) {
        Ln.d("reportMediaQualityMetrics");
        postMetric(getMediaQualityEventBuilder().addCall(call).setMediaQualityEvent(metrics).build());
    }

    public void reportMercuryConnectionStatus(CallImpl call, boolean connected) {
        Ln.d("reportMercuryConnectionStatus connected=" + connected);
        if (connected) {
            postMetric(getClientEventBuilder().addCall(call).mercuryConnectionRestored().build());
        } else {
            postMetric(getClientEventBuilder().addCall(call).mercuryConnectionLost().build());
        }
    }
}
