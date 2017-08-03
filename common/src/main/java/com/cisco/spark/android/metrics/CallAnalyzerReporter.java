package com.cisco.spark.android.metrics;

import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.callcontrol.model.Call;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.media.MediaType;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.reachability.NetworkReachability;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wx2.diagnostic_events.ClientEvent;
import com.github.benoitdion.ln.Ln;

import retrofit2.Response;

/*
 * An implementation of https://sqbu-github.cisco.com/WebExSquared/event-dictionary
 * See https://sqbu-github.cisco.com/WebExSquared/event-dictionary/wiki/Event-Definitions for more
 * details
 */
public class CallAnalyzerReporter {
    private final DeviceRegistration deviceReg;
    private final ApiTokenProvider tokenProvider;
    private final OperationQueue operationQueue;
    private final UserAgentProvider uaProvider;
    private final TrackingIdGenerator trackingIdGenerator;
    private final NetworkReachability networkReachability;

    public CallAnalyzerReporter(final DeviceRegistration deviceReg, final ApiTokenProvider tokenProvider,
                                final OperationQueue operationQueue, final UserAgentProvider uaProvider,
                                final TrackingIdGenerator trackingIdGenerator, final NetworkReachability networkReachability) {
        this.deviceReg = deviceReg;
        this.tokenProvider = tokenProvider;
        this.operationQueue = operationQueue;
        this.uaProvider = uaProvider;
        this.trackingIdGenerator = trackingIdGenerator;
        this.networkReachability = networkReachability;
    }

    private CallAnalyzerBuilder getBuilder() {
        try {
            return new CallAnalyzerBuilder(deviceReg, tokenProvider.getAuthenticatedUser(), uaProvider, trackingIdGenerator, networkReachability);
        } catch (NotAuthenticatedException e) {
            Ln.e(e, "No authenticated user?!");
            return new CallAnalyzerBuilder(deviceReg, null, uaProvider, trackingIdGenerator, networkReachability);
        }
    }

    private void postMetric(GenericMetric metric) {
        if (metric != null) {
            operationQueue.postGenericMetric(metric);
        }
    }

    // Should be called by: Client
    public void reportClientStartedFromCrash() {
        // If we can pass in info on the last call we were in that would be a bonus
        postMetric(getBuilder().clientCrash().build());
    }

    // Should be called by: Client
    public void reportMediaEngineCrash() {
        // If we can pass in info on the last call we were in that would be a bonus
        postMetric(getBuilder().mediaEngineCrash().build());
    }

    // Should be called by: Client UI (but is currently SDK)
    public void reportCallInitiated(Call call) {
        // This SHOULD be reported from the UI the moment you hit the button to initiate a call,
        // but for now I'm putting it in CCS
        postMetric(getBuilder().addCall(call).callInitiated(call).build());
    }

    // Should be called by: SDK
    public void reportLocalSdpGenerated(Call call) {
        postMetric(getBuilder().addCall(call).localSdpGenerated().build());
    }

    // Should be called by: SDK
    public void reportJoinRequest(String callId, LocusKey locusKey) {
        postMetric(getBuilder().addCall(callId, locusKey).joinRequest().build());
    }

    // Should be called by: SDK
    public void reportJoinResponseSuccess(String callId, LocusKey locusKey) {
        postMetric(getBuilder().addCall(callId, locusKey).joinResponse(true).build());
    }

    // Should be called by: SDK
    public void reportJoinResponseError(String callId, LocusKey locusKey, @Nullable Response response) {
        postMetric(
                getBuilder()
                        .addCall(callId, locusKey)
                        .joinResponse(false)
                        .addLocusErrorResponse(true, response)
                        .build()
        );
    }

    // Should be called by: SDK
    public void reportMediaEngineReady(Call call) {
        postMetric(getBuilder().addCall(call).mediaEngineReady().build());
    }

    // Should be called by: SDK
    public void reportRemoteSdpReceived(Call call) {
        postMetric(getBuilder().addCall(call).remoteSDPRx().build());
    }

    // Should be called by: SDK
    public void reportClientNotificationReceived(LocusKey locusKey, boolean mercury) {
        postMetric(getBuilder().addCall(null, locusKey).addTrigger(mercury ? ClientEvent.Trigger.MERCURY_EVENT : ClientEvent.Trigger.OTHER)
            .notificationReceived().build());
    }

    // Should be called by: Client UI
    public void reportCallDisplayed(Call call) {
        // "A call was displayed to the user. This could be the full screen UI if the user joined, a join button or otherwise. A client may fire this event multiple times in a call."
        postMetric(getBuilder().addCall(call).callDisplayedToUser().build());
    }

    // Should be called by: Client?
    public void reportCallAlertDisplayed(LocusKey locusKey) {
        // "A call invite/alert was displayed to the user. A ringtone is also a form of alert even if no UI is presented."
        postMetric(getBuilder().addCall(null, locusKey).callAlertDisplayed().build());
    }

    // Should be called by: Client?
    public void reportCallAlertRemoved(LocusKey locusKey) {
        // "A call alert was removed. Likely this is due to the user answering the call on this device, on another device or the call ending before the user acted.
        // It could also be because the user declined to answer the incoming call, or never answered it and it timed out.
        // Details in the event specify which of these occurred and where (e.g. triggered, displayLocation)"
        postMetric(getBuilder().addCall(null, locusKey).callAlertRemoved().build());
    }

    // Should be called by: SDK
    public void reportIceStart(Call call) {
        postMetric(getBuilder().addCall(call).iceStart().build());
    }

    // Should be called by: SDK
    public void reportIceEnd(Call call, boolean success) {
        postMetric(getBuilder().addCall(call).iceEnd(success).build());
    }

    // Should be called by: SDK
    public void reportMediaRxStart(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).rxMediaStart(mediaType).build());
    }

    // Should be called by: SDK
    // Need to clarify if this is whole session stopped, or just a transient stop (i.e. something like WME video blocked)
    public void reportMediaRxStop(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).rxMediaStop(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMediaTxStart(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).txMediaStart(mediaType).build());
    }

    // Should be called by: SDK
    // Need to clarify if this is whole session stopped, or just a transient stop  (i.e. something like WME video blocked)
    public void reportMediaTxStop(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).txMediaStop(mediaType).build());
    }

    // Should be called by: Client UI
    public void reportMediaRenderStart(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).mediaRenderStart(mediaType).build());
    }

    // Should be called by: Client UI
    public void reportMediaRenderStop(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).mediaRenderStop(mediaType).build());
    }

    // Should be called by: SDK
    public void reportShareInitiated(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).shareInitiated(mediaType).build());
    }

    // Should be called by: SDK
    public void reportShareStopped(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).shareStopped(mediaType).build());
    }

    // Should be called by: Client UI
    public void reportShareSelectedApp(Call call) {
        postMetric(getBuilder().addCall(call).shareAppSelected().build());
    }

    // Should be called by: SDK
    public void reportMuted(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).muted(mediaType).build());
    }

    // Should be called by: SDK
    public void reportUnmuted(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).unmuted(mediaType).build());
    }

    // Should be called by: Client UI? Perhaps SDK though not enough info atm I think
    public void reportCallLeaveFromUi(Call call) {
        postMetric(getBuilder().addCall(call).callLeftFromUi().build());
    }

    // Should be called by: SDK probably? Not really sure what this means
    public void reportCallRemoteStarted(LocusKey locusKey) {
        // TODO: This event needs to be clarified
        // Incoming call?
        // I think this was initially intended as a more general 'a meeting has started in
        // a space the user is a member of - render the JOIN button'
        postMetric(getBuilder().callRemoteStarted().build());
    }

    // Should be called by: SDK probably? Not really sure what this means
    public void reportCallRemoteEnded(LocusKey locusKey) {
        // TODO: This event needs to be clarified
        // See comments in above function.
        // Locus remotely ending the call? Leaving the call due to being last participant?
        // Or just stop rendering a join button for a space because the meeting in there ended?
        postMetric(getBuilder().callRemoteEnded().build());
    }

    // SCA/SRC reports - presumably they will want details of what was in the SCA/SCR?
    // is there a standard format?

    // Should be called by: SDK
    public void reportMultistreamScaTx(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).scaTx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScaRx(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).scaRx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScrTx(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).scrTx(mediaType).build());
    }

    // Should be called by: SDK
    public void reportMultistreamScrRx(Call call, MediaType mediaType) {
        postMetric(getBuilder().addCall(call).scrRx(mediaType).build());
    }

    // --------------- CMR related diagnostics - usage TBA in future, so ignore for now ------------

    // Should be called by: Client?
    public void reportPinPromptShown(Call call) {
        // So this should probably come from the UI?
        postMetric(getBuilder().addCall(call).pinPromptShown().build());
    }

    // Should be called by: SDK?
    public void reportPinEntered(Call call) {
        postMetric(getBuilder().addCall(call).pinEntered().build());
    }

    // Should be called by: SDK or Client?
    public void reportMeetingLobbyEntered(Call call) {
        // AMBIGUITY: Is this just the "waiting for host" screen, or is it the whole PIN entry +
        // "waiting for host" process. This is TBA in the future
        postMetric(getBuilder().addCall(call).lobbyEntered().build());
    }

    // Should be called by: SDK or Client?
    public void reportMeetingLobbyExited(Call call) {
        // AMBIGUITY: Is this just the "waiting for host" screen, or is it the whole PIN entry +
        // "waiting for host" process. This is TBA in the future
        postMetric(getBuilder().addCall(call).lobbyExited().build());
    }

    // ---------------------------------------------------------------------------------------------
}
