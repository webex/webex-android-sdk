package com.cisco.spark.android.metrics;

import android.net.ConnectivityManager;
import android.support.annotation.Nullable;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.callcontrol.model.Call;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.media.MediaType;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.reachability.NetworkReachability;
import com.cisco.spark.android.util.NetworkUtils;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wx2.diagnostic_events.ClientError;
import com.cisco.wx2.diagnostic_events.ClientEvent;
import com.cisco.wx2.diagnostic_events.Error;
import com.cisco.wx2.diagnostic_events.Event;
import com.cisco.wx2.diagnostic_events.EventType;
import com.cisco.wx2.diagnostic_events.Origin;
import com.cisco.wx2.diagnostic_events.OriginTime;
import com.cisco.wx2.diagnostic_events.SparkIdentifiers;
import com.cisco.wx2.diagnostic_events.ValidationException;
import com.github.benoitdion.ln.Ln;

import org.joda.time.Instant;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import retrofit2.Response;

public class CallAnalyzerBuilder {
    private ClientEvent.Builder clientEventBuilder;
    private SparkIdentifiers.Builder identBuilder;

    private final UserAgentProvider uaProvider;
    private final TrackingIdGenerator trackingIdGenerator;
    private final NetworkReachability networkReachability;

    /*
     * Clients MUST always populate correlationId, userId, deviceId, and trackingId.
     * Clients SHOULD populate as many identifiers as possible when sending events.
     * This includes locusSessionId, locusId, locusStartTime, and any others available to the client.
     */

    public CallAnalyzerBuilder(final DeviceRegistration deviceReg, final AuthenticatedUser user,
                               final UserAgentProvider uaProvider, final TrackingIdGenerator trackingIdGenerator,
                               final NetworkReachability networkReachability) {
        this.uaProvider = uaProvider;
        this.trackingIdGenerator = trackingIdGenerator;
        this.networkReachability = networkReachability;

        clientEventBuilder = ClientEvent.builder();
        identBuilder = SparkIdentifiers.builder();

        if (user != null) {
            if (user.getUserId() != null) {
                try {
                    identBuilder.userId(UUID.fromString(user.getUserId()));
                } catch (IllegalArgumentException e) {
                }
            }

            identBuilder.orgId(user.getOrgId());
        }

        if (deviceReg.getDeviceIdentifier() != null) {
            identBuilder.deviceId(deviceReg.getDeviceIdentifier());
        } else {
            identBuilder.deviceId("-"); // required field. This is far from ideal. Should find something that always has a correct deviceID
        }

        identBuilder.correlationId("-"); // required field. Should be overwritten with something sensible if applicable
    }

    private static EventType.MediaType mediaTypeConversion(MediaType mediaType) {
        switch(mediaType) {
            case AUDIO:
                return EventType.MediaType.AUDIO;
            case VIDEO:
                return EventType.MediaType.VIDEO;
            case CONTENT_SHARE:
                return EventType.MediaType.SHARE;
            case WHITEBOARD:
                return EventType.MediaType.WHITEBOARD;
            default:
                // Urgh...well...just make sure it doesn't get here...
                return EventType.MediaType.AUDIO;
        }
    }

    public CallAnalyzerBuilder addCall(Call call) {
        if (call != null) {
            identBuilder.correlationId(call.getCallId());

            LocusKey key = call.getLocusKey();
            if (key != null) {
                try {
                    identBuilder.locusId(UUID.fromString(key.getLocusId()));
                } catch (IllegalArgumentException e) {
                    // LocusKey::getLocusId is almost always in UUID form, but in some tests it's not
                    // so just catch this and carry on - we just won't set a locusId in the metric
                }
            }

            if (call.getEpochStartTime() > 0) {
                identBuilder.locusStartTime(new Instant(call.getEpochStartTime()));
            }
        }

        return this;
    }

    public CallAnalyzerBuilder addCall(String correlationId, LocusKey locusKey) {
        if (correlationId != null) {
            identBuilder.correlationId(correlationId);
        }

        if (locusKey != null) {
            try {
                identBuilder.locusId(UUID.fromString(locusKey.getLocusId()));
            } catch (IllegalArgumentException e) {
                // LocusKey::getLocusId is almost always in UUID form, but in some tests it's not
                // so just catch this and carry on - we just won't set a locusId in the metric
            }
        }

        return this;
    }

    public CallAnalyzerBuilder addTrigger(ClientEvent.Trigger trigger) {
        clientEventBuilder.trigger(trigger);

        return this;
    }

    public CallAnalyzerBuilder callInitiated(Call call) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_INITIATED).canProceed(true);

        if (call.getLocusKey() == null && call.getInvitee() != null) {
            clientEventBuilder.eventData(Collections.singletonMap("invitee", call.getInvitee()));
        }

        return this;
    }

    public CallAnalyzerBuilder localSdpGenerated() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_LOCAL_SDP_GENERATED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder joinRequest() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOCUS_JOIN_REQUEST).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder joinResponse(boolean success) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOCUS_JOIN_RESPONSE).canProceed(success);

        return this;
    }

    public CallAnalyzerBuilder rxMediaStart(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RX_START)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder rxMediaStop(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RX_STOP)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder txMediaStart(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_TX_START)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder txMediaStop(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_TX_STOP)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder mediaRenderStart(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RENDER_START)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder mediaRenderStop(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_RENDER_STOP)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder shareInitiated(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_INITIATED)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder shareStopped(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_STOPPED)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder mediaEngineReady() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_READY).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder remoteSDPRx() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_REMOTE_SDP_RECEIVED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder iceStart() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ICE_START).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder iceEnd(boolean success) {
        // eventData should contain information about the candidates (todo..)
        // if there are no successful candidates, an ice.failed error should be included
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ICE_END).canProceed(success);

        if (!success) {
            try {
                ClientError iceError = ClientError.builder().name(ClientError.Name.ICE_FAILED).shownToUser(true).build();
                clientEventBuilder.errors(Collections.singletonList(iceError));
            } catch (ValidationException e) {
                Ln.e(e, "Validation error creating ClientError");
            }
        }

        return this;
    }

    public CallAnalyzerBuilder addLocusErrorResponse(boolean shownToUser, @Nullable Response r) {
        String description = null;
        if (r == null) {
            Ln.e("No Response to attach to the error.");
        } else {
            try {
                description = r.errorBody().string();
            } catch (IOException e) {
                // Not able to get body
                Ln.e(e, "Unable to get error response body.");
            }
        }

        ClientError error = null;
        try {
            error = ClientError.builder()
                    .name(ClientError.Name.LOCUS_RESPONSE)
                    .shownToUser(shownToUser)
                    .category(Error.Category.SIGNALING)
                    .fatal(r == null || r.code() != LocusService.HTTP_LOCKED)
                    .httpCode(r != null ? r.code() : null)
                    .errorDescription(description)
                    .build();
        } catch (ValidationException e) {
            Ln.e(e, "Unable to generate Locus error from response.");
        }

        if (error != null) {
            clientEventBuilder.errors(Collections.singletonList(error));
        }
        return this;
    }

    public CallAnalyzerBuilder muted(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MUTED)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder unmuted(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_UNMUTED)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder notificationReceived() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_NOTIFICATION_RECEIVED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder pinPromptShown() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_PIN_PROMPT).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder pinEntered() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_PIN_COLLECTED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder lobbyEntered() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOBBY_ENTERED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder lobbyExited() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_LOBBY_EXITED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder callLeftFromUi() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_LEAVE).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder scaRx(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCA_RX)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder scaTx(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCA_TX)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder scrRx(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCR_RX)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder scrTx(MediaType mediaType) {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MULTISTREAM_SCR_TX)
            .canProceed(true)
            .mediaType(mediaTypeConversion(mediaType));

        return this;
    }

    public CallAnalyzerBuilder clientCrash() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_STARTED_FROM_CRASH).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder mediaEngineCrash() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_MEDIA_ENGINE_CRASH).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder callDisplayedToUser() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_DISPLAYED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder callAlertDisplayed() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ALERT_DISPLAYED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder callAlertRemoved() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_ALERT_REMOVED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder shareAppSelected() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_SHARE_SELECTED_APP).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder callRemoteStarted() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_REMOTE_STARTED).canProceed(true);

        return this;
    }

    public CallAnalyzerBuilder callRemoteEnded() {
        clientEventBuilder.name(ClientEvent.Name.CLIENT_CALL_REMOTE_ENDED).canProceed(true);

        return this;
    }

    private Origin.NetworkType getNetworkType() {
        if (networkReachability.getNetworkInfo() != null) {
            switch (networkReachability.getNetworkInfo().getType()) {
                case ConnectivityManager.TYPE_ETHERNET:
                    return Origin.NetworkType.ETHERNET;
                case ConnectivityManager.TYPE_WIFI:
                    return Origin.NetworkType.WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_DUN:
                    return Origin.NetworkType.CELLULAR;
                default:
                    return Origin.NetworkType.UNKNOWN;
            }
        } else {
            return Origin.NetworkType.UNKNOWN;
        }
    }

    public GenericMetric build() {
        try {
            identBuilder.trackingId(trackingIdGenerator.nextTrackingId());

            ClientEvent clientEvent = clientEventBuilder.identifiers(identBuilder.build()).build();

            OriginTime originTime = OriginTime.builder()
                .triggered(Instant.now())
                .sent(Instant.now()) // this needs to be set to pass validation, but will be updated later when actually sent
                .build();

            Origin origin = Origin.builder()
                .name(Origin.Name.ENDPOINT)
                .userAgent(uaProvider.get())
                .buildType(BuildConfig.DEBUG ? Origin.BuildType.DEBUG : Origin.BuildType.PROD)
                .networkType(getNetworkType())
                .localIP(NetworkUtils.getLocalIpAddress())
                .build();

            Event event = Event.builder()
                .eventId(UUID.randomUUID())
                .version(1)
                .originTime(originTime)
                .origin(origin)
                .event(clientEvent)
                .build();

            return GenericMetric.buildDiagnosticMetric(event);
        } catch (ValidationException e) {
            Ln.e(e, "Failed to build valid diagnostic metric");
            Ln.e(e.getValidationError().getErrors().toString());
            return null;
        }
    }
}
