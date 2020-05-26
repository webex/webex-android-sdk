package com.ciscowebex.androidsdk.internal.metric;

import android.support.annotation.Nullable;
import com.cisco.wx2.diagnostic_events.*;
import com.ciscowebex.androidsdk.internal.ErrorDetail;
import com.ciscowebex.androidsdk.internal.media.MediaCapability;
import com.ciscowebex.androidsdk.internal.media.MediaConstraint;
import com.ciscowebex.androidsdk.internal.media.WMEngine;
import com.ciscowebex.androidsdk.internal.model.GenericMetricModel;
import com.ciscowebex.androidsdk.internal.model.LocusKeyModel;
import com.ciscowebex.androidsdk.internal.model.ReachabilityModel;
import com.ciscowebex.androidsdk.phone.internal.CallImpl;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.phone.internal.ReachabilityService;
import com.github.benoitdion.ln.Ln;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientEventBuilder extends DiagnosticsEventBuilder {

    private ClientEvent.Builder clientEventBuilder = ClientEvent.builder();

    public ClientEventBuilder(PhoneImpl phone) {
        super(phone);
    }

    private static EventType.MediaType mediaTypeConversion(WMEngine.Media mediaType) {
        switch(mediaType) {
            case Audio:
                return EventType.MediaType.AUDIO;
            case Video:
                return EventType.MediaType.VIDEO;
            case Sharing:
                return EventType.MediaType.SHARE;
        }
        return EventType.MediaType.AUDIO;
    }

    public ClientEventBuilder addCall(CallImpl call) {
        addCallInfo(call);
        return this;
    }

    public ClientEventBuilder addCall(String correlationId, LocusKeyModel locusKey) {
        addCallInfo(correlationId, locusKey);
        return this;
    }

    public ClientEventBuilder addMediaCapabilities(MediaCapability capabilities) {
        com.cisco.wx2.diagnostic_events.MediaCapability.Builder rxBuilder = getDefaultMediaCapabilityBuilder();
        com.cisco.wx2.diagnostic_events.MediaCapability.Builder txBuilder = getDefaultMediaCapabilityBuilder();
        rxBuilder.audio(capabilities.hasSupport(MediaConstraint.ReceiveAudio));
        rxBuilder.video(capabilities.hasSupport(MediaConstraint.ReceiveVideo));
        rxBuilder.share(capabilities.hasSupport(MediaConstraint.ReceiveSharing));
        txBuilder.audio(capabilities.hasSupport(MediaConstraint.SendAudio));
        txBuilder.video(capabilities.hasSupport(MediaConstraint.SendVideo));
        txBuilder.share(capabilities.hasSupport(MediaConstraint.SendSharing));
        try {
            clientEventBuilder.mediaCapabilities(MediaCapabilities.builder().rx(rxBuilder.build()).tx(txBuilder.build()).build());
        } catch (ValidationException e) {
            Ln.w(e, "Unable to add mediaCapabilities to event.");
        }
        return this;
    }

    private static com.cisco.wx2.diagnostic_events.MediaCapability.Builder getDefaultMediaCapabilityBuilder() {
        return com.cisco.wx2.diagnostic_events.MediaCapability.builder()
                .audio(false)
                .video(false)
                .share(false)
                .share_audio(false)
                .whiteboard(false);
    }

    public ClientEventBuilder addTrigger(ClientEvent.Trigger trigger) {
        clientEventBuilder.trigger(trigger);
        return this;
    }

    public ClientEventBuilder callInitiated(CallImpl call) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_INITIATED).canProceed(true);
        return this;
    }

    public ClientEventBuilder callDeclined() {
        // 'canProceed' is set to say whether the behavior is correct or not. In this case, it is correct
        // as the user is the one to decline
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_DECLINED).canProceed(true);
        return this;
    }

    public ClientEventBuilder localSdpGenerated() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_LOCAL_SDP_GENERATED).canProceed(true);
        return this;
    }

    public ClientEventBuilder localSdpGeneratedFailed(long errorcode, String description) {
        try {
            ClientError error = ClientError.builder()
                    .name(ClientError.Name.MEDIA_ENGINE)
                    .shownToUser(true)
                    .errorCode(CallAnalyzerErrorCode.UNKNOWN_CALL_FAILURE.getErrorCode())
                    .category(CallAnalyzerErrorCode.UNKNOWN_CALL_FAILURE.getCategory())
                    .fatal(CallAnalyzerErrorCode.UNKNOWN_CALL_FAILURE.isFatal())
                    .errorDescription("WME createSDP failure, errorcode = " + errorcode + ", description = " + description)
                    .build();
            clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_LOCAL_SDP_GENERATED).errors(Collections.singletonList(error)).canProceed(false);
        } catch (ValidationException e) {
            Ln.e(e, "localSdpGeneratedFailed, Validation error creating ClientError");
        }
        return this;
    }

    public ClientEventBuilder joinRequest() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOCUS_JOIN_REQUEST).canProceed(true);
        return this;
    }

    public ClientEventBuilder joinResponse(boolean success) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOCUS_JOIN_RESPONSE).canProceed(success);
        return this;
    }

    public ClientEventBuilder rxMediaStart(WMEngine.Media mediaType, Long csi) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RX_START)
                .canProceed(true)
                .csi(csi)
                .mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder rxMediaStop(WMEngine.Media mediaType, Integer mediaStatus) {
        try {
            if (mediaStatus != 0) {
                CallAnalyzerErrorCode caErrorCode = CallAnalyzerErrorCode.fromMediaStatusCode(mediaStatus);
                ClientError.Builder errorBuilder = ClientError.builder()
                        .name(ClientError.Name.MEDIA_SCA)
                        .errorCode(caErrorCode.getErrorCode())
                        .category(com.cisco.wx2.diagnostic_events.Error.Category.MEDIA)
                        .fatal(false)
                        .shownToUser(false)
                        .errorDescription("Media status code = " + mediaStatus);
                clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RX_STOP)
                        .canProceed(true)
                        .mediaType(mediaTypeConversion(mediaType))
                        .errors(Collections.singletonList(errorBuilder.build()));
            } else {
                clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RX_STOP)
                        .canProceed(true)
                        .mediaType(mediaTypeConversion(mediaType));
            }
        } catch (ValidationException e) {
            Ln.e(e, "Unable to generate media sca error metric.");
        }

        return this;
    }

    public ClientEventBuilder txMediaStart(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_TX_START).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder txMediaStop(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_TX_STOP).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder mediaRenderStart(WMEngine.Media mediaType, Long csi) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RENDER_START).canProceed(true).csi(csi).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder mediaRenderStop(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RENDER_STOP).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder shareCsiChanged(Long csi) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_SHARE_CSI_CHANGED).canProceed(true).csi(csi);
        return this;
    }

    public ClientEventBuilder shareInitiated(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_INITIATED).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder shareStopped(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_STOPPED).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder mediaEngineReady() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_READY).canProceed(true);
        return this;
    }

    public ClientEventBuilder remoteSDPRx() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_REMOTE_SDP_RECEIVED).canProceed(true);
        return this;
    }

    public ClientEventBuilder iceStart() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ICE_START).canProceed(true);
        return this;
    }

    public ClientEventBuilder iceEnd(boolean success, List<MediaLine> iceMediaLineList) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ICE_END).canProceed(success).mediaLines(iceMediaLineList);
        if (!success) {
            try {
                ClientError iceError = ClientError.builder()
                        .name(ClientError.Name.ICE_FAILED)
                        .errorCode(CallAnalyzerErrorCode.ICE_FAILURE.getErrorCode())
                        .category(CallAnalyzerErrorCode.ICE_FAILURE.getCategory())
                        .fatal(CallAnalyzerErrorCode.ICE_FAILURE.isFatal())
                        .shownToUser(true)
                        .build();
                clientEventBuilder.errors(Collections.singletonList(iceError));
            } catch (ValidationException e) {
                Ln.e(e, "Validation error creating ClientError");
            }
        }
        return this;
    }

    public ClientEventBuilder addLocusErrorResponse(boolean shownToUser, String errorInfo) {
        ClientError.Builder builder = ClientError.builder()
                .name(ClientError.Name.LOCUS_RESPONSE)
                .shownToUser(shownToUser)
                .errorCode(CallAnalyzerErrorCode.NETWORK_UNAVAILABLE.getErrorCode())
                .category(CallAnalyzerErrorCode.NETWORK_UNAVAILABLE.getCategory())
                .fatal(CallAnalyzerErrorCode.NETWORK_UNAVAILABLE.isFatal());
        ClientError error = null;
        try {
            error = builder.build();
        } catch (ValidationException e) {
            Ln.e(e, "Unable to generate Locus error from response.");
        }
        if (error != null) {
            clientEventBuilder.errors(Collections.singletonList(error));
        }
        clientEventBuilder.eventData(Collections.singletonMap("Description", errorInfo));

        return this;
    }

    public ClientEventBuilder addLocusErrorResponse(boolean shownToUser, int httpCode, @Nullable ErrorDetail detail) {
        ClientError.Builder builder = ClientError.builder().name(ClientError.Name.LOCUS_RESPONSE).shownToUser(shownToUser);
        if (httpCode != -1) {
            builder.httpCode(httpCode);
        }
        // Response
        CallAnalyzerErrorCode errorCode = CallAnalyzerErrorCode.UNKNOWN_CALL_FAILURE;
        if (detail != null) {
            errorCode = CallAnalyzerErrorCode.fromErrorDetailCustomErrorCode(detail.extractCustomErrorCode());
            String errorDescription = detail.getMessage();
            if (errorCode == CallAnalyzerErrorCode.UNKNOWN_CALL_FAILURE) {
                // Add LocusErrorCode to the string (to know which error codes are not processed properly)
                errorDescription = errorDescription + " (" + detail.getErrorCode() + ")";
            }
            builder.errorDescription(errorDescription);
        } else {
            if (httpCode == -1) {
                // in this case (detail = null and httpCode = -1), it means network exception occurred
                errorCode = CallAnalyzerErrorCode.NETWORK_UNAVAILABLE;
            }
        }
        builder.errorCode(errorCode.getErrorCode()).category(errorCode.getCategory()).fatal(errorCode.isFatal());
        ClientError error = null;
        try {
            error = builder.build();
        } catch (ValidationException e) {
            Ln.e(e, "Unable to generate Locus error from response.");
        }
        if (error != null) {
            clientEventBuilder.errors(Collections.singletonList(error));
        }
        return this;
    }

    public ClientEventBuilder addTimeoutError(ClientError.Name name) {
        ClientError.Builder builder = ClientError.builder()
                .name(name)
                .errorCode(CallAnalyzerErrorCode.TIMEOUT.getErrorCode())
                .category(CallAnalyzerErrorCode.TIMEOUT.getCategory())
                .fatal(CallAnalyzerErrorCode.TIMEOUT.isFatal())
                .shownToUser(true);
        try {
            clientEventBuilder.errors(Collections.singletonList(builder.build()));
        } catch (ValidationException e) {
            Ln.e(e, "Unable to generate timeout error metric.");
        }
        return this;
    }

    public ClientEventBuilder addMediaDeviceError(int mediaErrorCode) {
        if (mediaErrorCode == 0) {
            return this;
        }
        CallAnalyzerErrorCode caErrorCode = CallAnalyzerErrorCode.fromMediaError(mediaErrorCode);
        ClientError.Builder builder = ClientError.builder()
                .name(ClientError.Name.MEDIA_DEVICE)
                .errorCode(caErrorCode.getErrorCode())
                .category(com.cisco.wx2.diagnostic_events.Error.Category.MEDIA)
                .fatal(caErrorCode.isFatal())
                .errorDescription("Media Error Code = " + mediaErrorCode)
                .shownToUser(true);
        try {
            clientEventBuilder.errors(Collections.singletonList(builder.build()));
        } catch (ValidationException e) {
            Ln.e(e, "Unable to generate media device error metric.");
        }
        return this;
    }

    public ClientEventBuilder muted(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MUTED).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder unmuted(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_UNMUTED).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder notificationReceived() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_NOTIFICATION_RECEIVED).canProceed(true);
        return this;
    }

    public ClientEventBuilder pinPromptShown() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_PIN_PROMPT).canProceed(true);
        return this;
    }

    public ClientEventBuilder pinEntered() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_PIN_COLLECTED).canProceed(true);
        return this;
    }

    public ClientEventBuilder lobbyEntered() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOBBY_ENTERED).canProceed(true);
        return this;
    }

    public ClientEventBuilder lobbyExited() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOBBY_EXITED).canProceed(true);
        return this;
    }

    public ClientEventBuilder callLeft(boolean canProceed) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_LEAVE).canProceed(canProceed);
        return this;
    }

    public ClientEventBuilder scaRx(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCA_RX).canProceed(true).mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public ClientEventBuilder scaTx(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCA_TX).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder scrRx(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCR_RX).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder scrTx(WMEngine.Media mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCR_TX).canProceed(true).mediaType(mediaTypeConversion(mediaType));
        return this;
    }

    public ClientEventBuilder clientCrash() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_STARTED_FROM_CRASH).canProceed(true);
        return this;
    }

    public ClientEventBuilder mediaEngineCrash() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_CRASH).canProceed(true);
        return this;
    }

    public ClientEventBuilder callDisplayedToUser() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_DISPLAYED).canProceed(true);
        return this;
    }

    public ClientEventBuilder callAlertDisplayed() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ALERT_DISPLAYED).canProceed(true);
        return this;
    }

    public ClientEventBuilder callAlertRemoved() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ALERT_REMOVED).canProceed(true);
        return this;
    }

    public ClientEventBuilder shareAppSelected() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_SELECTED_APP).canProceed(true);
        return this;
    }

    public ClientEventBuilder callRemoteStarted() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_REMOTE_STARTED).canProceed(true);
        return this;
    }

    public ClientEventBuilder callRemoteEnded() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_REMOTE_ENDED).canProceed(true);
        return this;
    }

    public ClientEventBuilder mediaCapabilities() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_CAPABILITIES).canProceed(true);
        return this;
    }

    public ClientEventBuilder callNetworkChanged() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_NETWORK_CHANGED).canProceed(true);
        return this;
    }

    public ClientEventBuilder floorGrantRequest() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_FLOOR_GRANT_REQUEST).canProceed(true);
        return this;
    }

    public ClientEventBuilder floorGrantedLocal() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_FLOOR_GRANTED_LOCAL).canProceed(true);
        return this;
    }

    public ClientEventBuilder callAppEnteringBackground() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ENTERING_BACKGROUND).canProceed(true);
        return this;
    }

    public ClientEventBuilder callAppEnteringForeground() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ENTERING_FOREGROUND).canProceed(true);
        return this;
    }

    public ClientEventBuilder callMoveMedia() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_MOVE_MEDIA).canProceed(true);
        return this;
    }

    public ClientEventBuilder addReachabilityStatus(ReachabilityService linusReachabilityService) {
        Map<String, ReachabilityModel> results = linusReachabilityService.getFeedback() == null ? null : linusReachabilityService.getFeedback().reachability;
        ClientEvent.ReachabilityStatus reachabilityStatus = getReachabilityStatus(results);
        Map<String, Object> data = new HashMap<>();
        if (results != null) {
            for (Map.Entry<String, ReachabilityModel> entry : results.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }
        clientEventBuilder.reachabilityStatus(reachabilityStatus)
                .canProceed(reachabilityStatus != ClientEvent.ReachabilityStatus.ALL_FALSE && reachabilityStatus != ClientEvent.ReachabilityStatus.NONE)
                .eventData(data);
        return this;
    }

    private static ClientEvent.ReachabilityStatus getReachabilityStatus(Map<String, ReachabilityModel> results) {
        boolean allFalse = true, allSuccess = true, allObjects = true;
        if (results == null) {
            Ln.d("No reachability results.");
            return ClientEvent.ReachabilityStatus.NONE;
        }
        for (ReachabilityModel value : results.values()) {
            allObjects = false;
            if (value.udp != null && value.udp.reachable) {
                allFalse = false;
            } else {
                allSuccess = false;
            }
            if (value.tcp != null && value.tcp.reachable) {
                allFalse = false;
            } else {
                allSuccess = false;
            }
            if (value.xtls != null && value.xtls.reachable) {
                allFalse = false;
            } else {
                allSuccess = false;
            }
            if (value.https != null && value.https.reachable) {
                allFalse = false;
            } else {
                allSuccess = false;
            }
        }
        if (allObjects) {
            Ln.e("ReachabilityStatus objects were not parsed properly.");
            return ClientEvent.ReachabilityStatus.NONE;
        } else if (allFalse) {
            return ClientEvent.ReachabilityStatus.ALL_FALSE;
        } else if (allSuccess) {
            return ClientEvent.ReachabilityStatus.ALL_SUCCESS;
        } else {
            return ClientEvent.ReachabilityStatus.PARTIAL_SUCCESS;
        }
    }

    public ClientEventBuilder shareDisplayed() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_LAYOUT_DISPLAYED).canProceed(true);
        return this;
    }

    public ClientEventBuilder pstnAudioConnectionSkipped() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_PSTNAUDIO_ATTEMPT_SKIP).canProceed(true);
        return this;
    }

    public ClientEventBuilder pstnAudioConnectionFinish() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_PSTNAUDIO_ATTEMPT_FINISH).canProceed(true);
        return this;
    }

    public ClientEventBuilder mediaReconnecting() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RECONNECTING).canProceed(true);
        return this;
    }

    public ClientEventBuilder mediaRecovered(boolean newSession) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RECOVERED).canProceed(true).recoveredBy(newSession ? ClientEvent.RecoveredBy.NEW : ClientEvent.RecoveredBy.RETRY);
        return this;
    }

    public ClientEventBuilder callAborted() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_ABORTED).canProceed(true);
        return this;
    }

    public ClientEventBuilder mercuryConnectionLost() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MERCURY_CONNECTION_LOST).canProceed(true);
        return this;
    }

    public ClientEventBuilder mercuryConnectionRestored() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MERCURY_CONNECTION_RESTORED).canProceed(true);
        return this;
    }

    public GenericMetricModel build() {
        return super.build(clientEventBuilder);
    }
}
