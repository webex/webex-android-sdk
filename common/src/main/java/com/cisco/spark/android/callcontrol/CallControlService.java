package com.cisco.spark.android.callcontrol;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.R;
import com.cisco.spark.android.callcontrol.events.CallControlActiveSpeakerChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlAudioOnlyStateChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlCallCancelledEvent;
import com.cisco.spark.android.callcontrol.events.CallControlCallConnectedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent;
import com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent.JoinType;
import com.cisco.spark.android.callcontrol.events.CallControlCallStartedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlDisableVideoEvent;
import com.cisco.spark.android.callcontrol.events.CallControlDisconnectedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlEndLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlEndWhiteboardShare;
import com.cisco.spark.android.callcontrol.events.CallControlFloorGrantedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlFloorReleasedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlHeldEvent;
import com.cisco.spark.android.callcontrol.events.CallControlInvalidLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlJoinedLobbyEvent;
import com.cisco.spark.android.callcontrol.events.CallControlJoinedMeetingFromLobbyEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLeaveLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalAudioMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusCreatedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusPmrChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLostEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMediaDecodeSizeChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsExpelEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingNotStartedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlModeratorMutedParticipantEvent;
import com.cisco.spark.android.callcontrol.events.CallControlNumericDialingPreventedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantJoinedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlPhoneStateChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlReconnectEvent;
import com.cisco.spark.android.callcontrol.events.CallControlResumedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlSelfParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlUserIsNotAuthorized;
import com.cisco.spark.android.callcontrol.events.CallControlViewDesktopShare;
import com.cisco.spark.android.callcontrol.events.CallControlViewWhiteboardShare;
import com.cisco.spark.android.callcontrol.model.Call;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.PermissionsHelper;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.events.CallNotificationEvent;
import com.cisco.spark.android.events.CallNotificationRemoveEvent;
import com.cisco.spark.android.events.CallNotificationType;
import com.cisco.spark.android.events.CallNotificationUpdateEvent;
import com.cisco.spark.android.events.RequestCallingPermissions;
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.events.AnsweredInactiveCallEvent;
import com.cisco.spark.android.locus.events.CallControlLocusRequiresModeratorPINOrGuest;
import com.cisco.spark.android.locus.events.CallControlLocusRequiresModeratorPINorGuestPIN;
import com.cisco.spark.android.locus.events.ConflictErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.ErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.FloorGrantedEvent;
import com.cisco.spark.android.locus.events.FloorLostEvent;
import com.cisco.spark.android.locus.events.FloorReleasedEvent;
import com.cisco.spark.android.locus.events.HighVolumeErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.IncomingCallEvent;
import com.cisco.spark.android.locus.events.InvalidLocusEvent;
import com.cisco.spark.android.locus.events.JoinedLobbyEvent;
import com.cisco.spark.android.locus.events.JoinedMeetingFromLobbyEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheChangedEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheReplacesEvent;
import com.cisco.spark.android.locus.events.LocusInviteesExceedMaxSizeEvent;
import com.cisco.spark.android.locus.events.LocusLeaveFailedEvent;
import com.cisco.spark.android.locus.events.LocusLeftEvent;
import com.cisco.spark.android.locus.events.LocusMeetingLockedEvent;
import com.cisco.spark.android.locus.events.LocusPmrChangedEvent;
import com.cisco.spark.android.locus.events.LocusUrlUpdatedEvent;
import com.cisco.spark.android.locus.events.LocusUserIsNotAuthorized;
import com.cisco.spark.android.locus.events.ParticipantChangedEvent;
import com.cisco.spark.android.locus.events.ParticipantDeclinedEvent;
import com.cisco.spark.android.locus.events.ParticipantJoinedEvent;
import com.cisco.spark.android.locus.events.ParticipantLeftEvent;
import com.cisco.spark.android.locus.events.ParticipantSelfChangedEvent;
import com.cisco.spark.android.locus.events.SuccessJoiningLocusEvent;
import com.cisco.spark.android.locus.model.CalliopeSupplementaryInformation;
import com.cisco.spark.android.locus.model.Floor;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusLink;
import com.cisco.spark.android.locus.model.LocusMeetingInfo;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantControls;
import com.cisco.spark.android.locus.model.LocusParticipantDevice;
import com.cisco.spark.android.locus.model.LocusRecordControl;
import com.cisco.spark.android.locus.model.LocusReplaces;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.model.MediaConnection;
import com.cisco.spark.android.locus.model.MediaInfo;
import com.cisco.spark.android.locus.model.MediaShare;
import com.cisco.spark.android.locus.requests.LocusInvitee;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.locus.service.MediaCreatedEvent;
import com.cisco.spark.android.locus.service.MediaUpdatedEvent;
import com.cisco.spark.android.log.Lns;
import com.cisco.spark.android.log.LogCallIndex;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.log.UploadLogsService;
import com.cisco.spark.android.media.CallControlMediaSessionStoppedEvent;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaRequestSource;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.media.MediaSessionCallbacks;
import com.cisco.spark.android.media.MediaSessionUtils;
import com.cisco.spark.android.media.MediaStartedEvent;
import com.cisco.spark.android.media.MediaType;
import com.cisco.spark.android.media.events.DeviceCameraUnavailable;
import com.cisco.spark.android.media.events.MediaActiveSpeakerChangedEvent;
import com.cisco.spark.android.media.events.MediaActiveSpeakerVideoMuted;
import com.cisco.spark.android.media.events.NetworkCongestionEvent;
import com.cisco.spark.android.media.events.NetworkDisableVideoEvent;
import com.cisco.spark.android.media.events.NetworkDisconnectEvent;
import com.cisco.spark.android.media.events.NetworkLostEvent;
import com.cisco.spark.android.media.events.NetworkReconnectEvent;
import com.cisco.spark.android.media.events.StunTraceResultEvent;
import com.cisco.spark.android.media.statistics.MediaStats;
import com.cisco.spark.android.meetings.LocusMeetingInfoProvider;
import com.cisco.spark.android.mercury.events.DeclineReason;
import com.cisco.spark.android.mercury.events.RoapMessageEvent;
import com.cisco.spark.android.metrics.CallAnalyzerReporter;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.metrics.value.CallJoinMetricValue;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.roap.model.RoapBaseMessage;
import com.cisco.spark.android.roap.model.RoapOfferMessage;
import com.cisco.spark.android.roap.model.RoapSession;
import com.cisco.spark.android.roap.model.RoapSessionCallbacks;
import com.cisco.spark.android.room.RoomUpdatedEvent;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.Action0;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.util.LinusReachabilityService;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.Toaster;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.CANCELLED_BY_LOCAL_ERROR;
import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.CANCELLED_BY_LOCAL_USER;
import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.DECLINED_BY_REMOTE_USER;
import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.DIAL_TIMEOUT_REACHED;
import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.ENDED_BY_LOCUS;
import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.ENDED_BY_REMOTE_USER;
import static com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent.JOIN;
import static com.cisco.spark.android.model.ErrorDetail.CustomErrorCode.LocusUserIsNotAuthorized;
import static com.cisco.spark.android.sync.ConversationContentProviderOperation.updateConversationInActiveCall;
import static com.cisco.spark.android.sync.ConversationContentProviderOperation.updateConversationLocusUrl;
import static com.cisco.spark.android.util.UIUtils.autoRotationEnabled;
import static com.cisco.spark.android.whiteboard.util.WhiteboardUtils.getIdFromUrl;

/**
 * The purpose of this class is to provide a general call control abstraction that's responsible for orchestrating
 * the activities of the locus signalling and media engine components.
 */
public class CallControlService implements MediaSessionCallbacks, RoapSessionCallbacks {
    public static final String AUDIO_TYPE = "AUDIO";
    public static final String VIDEO_TYPE = "VIDEO";

    protected final LocusService locusService;
    protected final CallMetricsReporter callMetricsReporter;
    protected final CallAnalyzerReporter callAnalyzerReporter;
    protected final EventBus bus;
    protected final LocusDataCache locusDataCache;
    protected final Context context;
    protected final CallUi callUi;
    protected final Settings settings;
    protected final NaturalLog ln;
    protected final Toaster toaster;

    private final MediaEngine mediaEngine;
    private final TrackingIdGenerator trackingIdGenerator;
    private final DeviceRegistration deviceRegistration;
    private final LogFilePrint logFilePrint;
    private final Gson gson;
    private final CoreFeatures coreFeatures;
    private final CallNotification callNotification;
    private final Provider<Batch> batchProvider;
    private final UploadLogsService uploadLogsService;
    private final PermissionsHelper permissionsHelper;
    private final LinusReachabilityService linusReachabilityService;
    private final SdkClient sdkClient;
    private final LocusMeetingInfoProvider locusMeetingInfoProvider;
    private final Object syncLock = new Object();
    private boolean hideSpinner;

    // joinedCalls we're actively joined to
    private final Map<String, Call> joinedCalls = new HashMap<>();

    private boolean dtmfReceiveSupported;
    protected LocusKey locusKey;

    private boolean wasAudioMuted;
    private MediaRequestSource wasVideoMuted;

    private Handler unansweredHandler;
    private boolean isCaller;
    private boolean audioMutedLocally;

    private boolean videoBlocked;
    private Action0 unblockedAction;

    private Timer lobbyKeepAliveTimer;
    private final Object lobbyTimerLock = new Object();


    public CallControlService(LocusService locusService, final MediaEngine mediaEngine, CallMetricsReporter callMetricsReporter,
                              EventBus bus, Context context,
                              TrackingIdGenerator trackingIdGenerator, DeviceRegistration deviceRegistration, LogFilePrint logFilePrint,
                              Gson gson, UploadLogsService uploadLogsService, CallNotification callNotification, LocusDataCache locusDataCache,
                              Settings settings, Provider<Batch> batchProvider, Ln.Context lnContext, CallUi callUi,
                              LinusReachabilityService linusReachabilityService, SdkClient sdkClient,
                              CallAnalyzerReporter callAnalyzerReporter, Toaster toaster, CoreFeatures coreFeatures, LocusMeetingInfoProvider locusMeetingInfoProvider) {
        this.locusService = locusService;
        this.mediaEngine = mediaEngine;
        this.callMetricsReporter = callMetricsReporter;
        this.bus = bus;
        this.context = context;
        this.trackingIdGenerator = trackingIdGenerator;
        this.deviceRegistration = deviceRegistration;
        this.logFilePrint = logFilePrint;
        this.gson = gson;
        this.callNotification = callNotification;
        this.locusDataCache = locusDataCache;
        this.batchProvider = batchProvider;
        this.callUi = callUi;
        this.uploadLogsService = uploadLogsService;
        this.settings = settings;
        this.coreFeatures = coreFeatures;
        this.ln = Ln.get(lnContext, "CallControlService");
        this.permissionsHelper = new PermissionsHelper(context);
        this.linusReachabilityService = linusReachabilityService;
        this.sdkClient = sdkClient;
        this.callAnalyzerReporter = callAnalyzerReporter;
        this.toaster = toaster;
        this.locusMeetingInfoProvider = locusMeetingInfoProvider;
        bus.register(this);
    }

    public void updateLocusWithPin(LocusKey locusKey, String pin) {
        // User must already be in the Lobby of the meeting from this device.
        LocusData locusData = locusDataCache.getLocusData(locusKey);
        if (this.locusKey.equals(locusKey) && locusData != null && locusData.getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
            locusService.updateLocusWithMeetingPin(locusKey, pin);
        }

        Call call = getCall(locusKey);

        if (call != null) {
            // Excluding any checks for if the user is actually in the lobby as this event is more concerned with
            // if the user entered a PIN into the UI
            callAnalyzerReporter.reportPinEntered(call);
        }
    }

    /**
     * Join call.  This is used for joining an existing locus or  making 1:1 call
     * to user/endpoint using locus /call api.
     * @param callContext callContext obect to start call with locus
     */
    public Call joinCall(final CallContext callContext) {
        Ln.i("CallControlService.joinCall, -------------------------------------------------------------------------------------------------------");
        Ln.i("CallControlService.joinCall, invitee = " + callContext.getInvitee() + ", locusKey = " + callContext.getLocusKey() + ", isOneOnOne = " + callContext.isOneOnOne());
        Ln.i("CallControlService.joinCall, -------------------------------------------------------------------------------------------------------");


        // if making call to uri then don't allow dialing numeric uris if associated feature is not enabled
        if (!TextUtils.isEmpty(callContext.getInvitee())) {
            if (!coreFeatures.isNumericDialingEnabled() && Strings.isPhoneNumber(callContext.getInvitee())) {
                callMetricsReporter.reportCallNumericDialPrevented();
                bus.post(new CallControlNumericDialingPreventedEvent());
                return null;
            }
        }

        if (!permissionsHelper.hasCameraPermission() || !permissionsHelper.hasMicrophonePermission()) {
            if (callContext.isFromNotification() || callContext.isCrossLaunch()) {
                callUi.requestCallPermissions(callContext);
            } else {
                bus.post(new RequestCallingPermissions(callContext));
            }
            return null;
        }

        Call call = Call.createCall(callContext);
        call.setActive(true);

        synchronized (joinedCalls) {
            joinedCalls.put(call.getCallId(), call);
            Ln.i("CallControlService.joinCall, call ID = " + call.getCallId());
        }

        callAnalyzerReporter.reportCallInitiated(call);

        // TODO remove need for this...still some uses as we transition
        this.locusKey = callContext.getLocusKey();


        logCall(LogCallIndex.JOINING, call.getLocusData());

        checkJoinRoomCall(call, callContext);

        return call;
    }

//    public Call joinCall(final CallContext callContext) {
//        return joinCall(callContext, true);
//    }

    public void joinDesktopShare(LocusKey locusKey) {
        CallContext callContext = new CallContext.Builder(locusKey)
                .setShowFullScreen(true)
                .setMediaDirection(MediaEngine.MediaDirection.SendReceiveShareOnly)
                .build();

        joinCall(callContext);

        Call call = getCall(locusKey);
        if (call == null || call.getLocusData() == null || call.getLocusData().getLocus() == null) return;

        LocusData locusData = call.getLocusData();
        MediaShare mediaShare = locusData.getLocus().getGrantedFloor();
        if (mediaShare != null && mediaShare.isContent()) {
            bus.post(new CallControlViewDesktopShare(locusKey));
        }

        MediaSession mediaSession = call.getMediaSession();
        if (mediaSession != null) {
            mediaSession.joinShare(locusData.getFloorGrantedId());
        } else {
            Floor floor = mediaShare != null ? mediaShare.getFloor() : null;
            LocusParticipant selfParticipant = locusData.getLocus().getSelf();
            if (floor != null && selfParticipant != null) {
                floor.setBeneficiary(selfParticipant);
            }
        }
    }

    /**
     * Check Join Room Call.  This decides if the call should use a room for media in SquaredCallControlService.
     */
//    public void checkJoinRoomCall(Call call, CallContext callContext, boolean showCallUI) {
//        if (showCallUI) {
//            callUi.showInCallUi(callContext, false);
//        }
//        startMediaSession(call, callContext.getMediaDirection());
//    }

    public void checkJoinRoomCall(Call call, CallContext callContext) {

        callUi.showInCallUi(callContext, false);
        startMediaSession(call, callContext.getMediaDirection());
    }

    protected void startMediaSession(Call call, MediaEngine.MediaDirection mediaDirection) {
        Ln.i("CallControlService.startMediaSession, call ID = " + call.getCallId());

        if (mediaDirection == null) {
            mediaDirection = MediaEngine.MediaDirection.SendReceiveAudioVideoShare;
        }

        requestAudioFocus(mediaDirection);

        MediaSession mediaSession = mediaEngine.createMediaSession(call.getCallId());
        call.setMediaSession(mediaSession);
        mediaSession.startSession(deviceRegistration.getDeviceSettingsString(), mediaDirection, this, sdp -> {
            if (sdp != null) {
                List<MediaConnection> mediaConnectionList = buildMediaConnectionList(call, sdp);

                Ln.d("Session, call connected = " + call.isCallConnected() + ", call started = " + call.isCallStarted());
                if (call.getLocusData() != null && call.getLocusData().getLocus().isJoinedFromThisDevice(deviceRegistration.getUrl())) {
                    locusService.modifyMedia(call.getLocusKey(), mediaConnectionList, mediaSession.isAudioMuted());
                } else {
                    if (call.getLocusKey() != null) {
                        locusService.joinLocus(call.getLocusKey(), call.getCallId(), mediaConnectionList, call.getUsingResource(), call.isMoveMediaToResource(), call.isAnsweringCall());
                    } else {
                        isCaller = true;
                        locusService.call(call.getInvitee(), call.getCallId(), call.getModerator(), call.getHostPin(), mediaConnectionList, call.getUsingResource(), call.isAnsweringCall());
                    }
                }
                String joinLocusTrackingID = trackingIdGenerator.currentTrackingId();
                call.setJoinLocusTrackingID(joinLocusTrackingID);

                callAnalyzerReporter.reportLocalSdpGenerated(call);
            }
        });
    }


    protected void updateMediaSession(Call call, MediaEngine.MediaDirection mediaDirection) {
        Ln.i("CallControlService.updateMediaSession, call ID = " + call.getCallId() + ", media direction = " + mediaDirection + ", mediaSession = " + call.getMediaSession());

        MediaSession mediaSession = call.getMediaSession();
        if (mediaSession != null) {
            mediaSession.updateSession(mediaDirection, sdp -> {
                if (sdp != null) {
                    Ln.i("CallControlService.updateMediaSession, onSDKReady");
                    List<MediaConnection> mediaConnectionList = buildMediaConnectionList(call, sdp);

                    LocusData locusData = call.getLocusData();
                    if (call != null && isInPairedCall(call) && !TextUtils.isEmpty(call.getUsingResource())) {
                        ln.i("CallControlService.onSDPReady, adding (share) media to existing paired call");
                        locusService.createMedia(call.getLocusKey(), mediaConnectionList.get(0));
                    } else if (call != null && locusData.getLocus().isJoinedFromThisDevice(deviceRegistration.getUrl()) && !isCopyingCallFromTp(call)) {
                        ln.i("CallControlService.onSDPReady, modifying media for existing call");
                        locusService.modifyMedia(call.getLocusKey(), mediaConnectionList, mediaSession.isAudioMuted());
                    }

                    callAnalyzerReporter.reportLocalSdpGenerated(call);
                }
            });
        }
    }


    private List<MediaConnection> buildMediaConnectionList(Call call, String sdp) {
        List<MediaConnection> mediaConnectionList = new ArrayList<>();
        String calliopeSupplementaryInformationString = buildCalliopeSupplementaryInformation();

        Map<String, Object> clusterInfo = linusReachabilityService.getLatestLinusReachabilityResults();

        MediaInfo mediaInfo;
        if (coreFeatures.isRoapEnabled()) {
            // send ROAP OFFER message
            mediaInfo = new MediaInfo(MediaEngine.SDP_TYPE, calliopeSupplementaryInformationString, clusterInfo);

            int roapSequenceNumber = 1;
            RoapSession roapSession = call.getRoapSession();
            if (roapSession == null) {
                roapSession = new RoapSession(roapSequenceNumber, RoapSession.SessionState.OFFER_SENT, this);
                call.setRoapSession(roapSession);
            } else {
                roapSequenceNumber = roapSession.getSeq() + 1;
                roapSession.setSeq(roapSequenceNumber);
                roapSession.setState(RoapSession.SessionState.OFFER_SENT);
            }

            List<String> sdpList = new ArrayList<>();
            sdpList.add(sdp);
            RoapOfferMessage roapOfferMessage = new RoapOfferMessage(roapSequenceNumber, sdpList, 0L);
            mediaInfo.setRoapMessage(roapOfferMessage);

        } else {
            // use traditional SDP offer/answer
            mediaInfo = new MediaInfo(sdp, MediaEngine.SDP_TYPE, calliopeSupplementaryInformationString, clusterInfo);
        }
        mediaInfo.setDtmfReceiveSupported(dtmfReceiveSupported);


        // set audio/videoMuted to whatever they were previously set to so that locus does not interpret this
        // as a toggle of those values (in which case it doesn't send updated SDP to Calliope)
        mediaInfo.setVideoMuted(call.getMediaSession().isVideoMuted());
        mediaInfo.setAudioMuted(call.getMediaSession().isAudioMuted());

        MediaConnection mediaConnection = new MediaConnection();
        if (call.getLocusData() != null && call.getLocusData().getLocus() != null) {
            MediaConnection currentMediaConnection = getMediaConnection(call.getLocusData().getLocus());
            if (currentMediaConnection != null)
                mediaConnection.setMediaId(currentMediaConnection.getMediaId());
            else
                Ln.w("Could not set media ID from current connection.");
        }
        mediaConnection.setType("SDP");
        mediaConnection.setLocalSdp(gson.toJson(mediaInfo));
        mediaConnectionList.add(mediaConnection);
        return mediaConnectionList;
    }

    private void endMediaSession(Call call, boolean logMediaStats) {
        if (call == null) {
            call = getActiveCall();
        }
        Ln.d("CallControlService.endMediaSession, locusKey = %s", call.getLocusKey());

        MediaSession mediaSession = call.getMediaSession();
        if (mediaSession != null) {
            if (logMediaStats) {
                // log media stats for ABC/ABS testing
                logEndMediaStats(mediaSession);
            }
            if (mediaSession.isScreenSharing()) {
                mediaSession.stopScreenShare(call != null ? call.getLocusData().getFloorGrantedId() : "");
            }

            mediaSession.stopMedia();
            mediaSession.endSession();

            // metrics reporting code will need access to media session info later on
            callMetricsReporter.setMediaSession(mediaSession);
            call.setMediaSession(null);

            abandonAudioFocus();

            bus.post(new CallControlMediaSessionStoppedEvent());
        }
    }


    @Override
    public void onICEComplete(String callId) {
        Ln.d("CallControlService.onICEComplete");

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportIceEnd(call, true);
        }
    }

    @Override
    public void onICEFailed(String callId) {
        Ln.d("CallControlService.onICEFailed");

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportIceEnd(call, false);

            cancelCall(call.getLocusKey(), CancelReason.LOCAL_ICE_FAILURE);

            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(() -> callUi.reportIceFailure(call));

            bus.post(new CallControlCallJoinErrorEvent());
        }
    }

    @Override
    public void onFirstPacketRx(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onFirstPacketRx %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMediaRxStart(call, mediaType);

            if (mediaType == MediaType.AUDIO) {
                callUi.dismissRingback(call);

                // also using this for now to indicate we've started rendering audio
                callAnalyzerReporter.reportMediaRenderStart(call, mediaType);
            }
        }
    }

    @Override
    public void onFirstPacketTx(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onFirstPacketTx %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMediaTxStart(call, mediaType);
        }
    }

    @Override
    public void onMediaRxStop(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onMediaRxStop %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMediaRxStop(call, mediaType);
        }
    }

    @Override
    public void onMediaTxStop(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onMediaTxStop %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMediaTxStop(call, mediaType);
        }
    }

    @Override
    public void onScrRx(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onScrRx %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMultistreamScrRx(call, mediaType);
        }
    }

    @Override
    public void onScaRx(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onScaRx %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMultistreamScaRx(call, mediaType);
        }
    }

    @Override
    public void onScrTx(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onScrTx %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMultistreamScrTx(call, mediaType);
        }
    }

    @Override
    public void onScaTx(String callId, MediaType mediaType) {
        Ln.d("CallControlService.onScaTx %s", mediaType);

        Call call = getCall(callId);

        if (call != null) {
            callAnalyzerReporter.reportMultistreamScaTx(call, mediaType);
        }
    }

    /**
     * This will get called from media engine. It is a trimmed down version of onMediaBlocked, just for changes in video of the active speaker.
     * Listeners can register a callback action to invoke when video changes from blocked to unblocked.  The significance of this transition
     * versus others is, full frame live video is available this action is called.
     */
    @Override
    public void onMediaBlocked(String callId, MediaType mediaType, boolean blocked) {
        Ln.d("CallControlService.onVideoBlocked(%b), videoBlocked=%b, unblockedAction=%s", blocked, videoBlocked, unblockedAction == null ? "null" : "non-null");
        if (videoBlocked && !blocked && unblockedAction != null)
            unblockedAction.call();
        videoBlocked = blocked;

        Call call = getCall(callId);
        if (call != null) {
            if (!blocked) {
                callAnalyzerReporter.reportMediaRenderStart(call, mediaType);
            } else {
                callAnalyzerReporter.reportMediaRenderStop(call, mediaType);
            }
        }
    }

    @Override
    public void onShareStopped(String callId) {
        Ln.d("CallControlService.onShareStopped");
    }

    public void registerVideoUnblockedAction(Action0 action) {
        Ln.d("CallControlService.registerVideoUnblockedAction");
        unblockedAction = action;
    }

    public void unregisterVideoUnblockedAction() {
        Ln.d("CallControlService.unregisterVideoUnblockedAction");
        unblockedAction = null;
    }

    private boolean isCopyingCallFromTp(Call call) {
        return isInPairedCall(call) && !TextUtils.isEmpty(call.getLocusData().getObservingResource()) && call.getUsingResource() == null;
    }

    /**
     * Build calliopeSupplementaryInformation. These are for internal testing use only
     */
    private String buildCalliopeSupplementaryInformation() {
        List<CalliopeSupplementaryInformation> calliopeSupplementaryInformationList = new ArrayList<CalliopeSupplementaryInformation>();

        // Custom Linus name
        String linusName = settings.getLinusName();
        if (!TextUtils.isEmpty(linusName)) {
            CalliopeSupplementaryInformation calliopeSupplementaryInformation = new CalliopeSupplementaryInformation("LINUS_SELECT", linusName);
            calliopeSupplementaryInformationList.add(calliopeSupplementaryInformation);
        }

        // Custom Call feature
        String customCallFeature = settings.getCustomCallFeature();
        if (!TextUtils.isEmpty(customCallFeature)) {
            CalliopeSupplementaryInformation calliopeSupplementaryInformation = new CalliopeSupplementaryInformation("CALL_FEATURE", customCallFeature);
            calliopeSupplementaryInformationList.add(calliopeSupplementaryInformation);
        }

        return gson.toJson(calliopeSupplementaryInformationList);
    }

    public void holdCall(LocusKey locusKey) {
        ln.i("CallControlService.holdCall");

        Call call = getCall(locusKey);
        if (call != null) {
            call.setOnHold(true);
            //locusService.holdLocus(locusKey);

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                mediaSession.stopMedia();
            }
        }
    }


    public void resumeCall(LocusKey locusKey) {
        ln.i("CallControlService.resumeCall");

        Call call = getCall(locusKey);
        if (call != null) {
            call.setOnHold(false);
            //locusService.resumeLocus(locusKey);

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                mediaSession.restartMedia();
            }
        }
    }


    public void mergeCalls(LocusKey currentLocusKey, LocusKey secondLocusKey) {
        ln.i("CallControlService.mergeCalls");

        Call secondCall = getCall(secondLocusKey);
        MediaSession mediaSession = secondCall.getMediaSession();
        if (secondCall != null) {
            if (mediaSession != null) {
                mediaSession.endSession();
            }
            locusService.mergeLoci(currentLocusKey, secondLocusKey);
        }
    }


    public void switchCalls(LocusKey currentLocusKey, LocusKey secondLocusKey) {
        ln.i("CallControlService.switchCalls");

        holdCall(currentLocusKey);
        resumeCall(secondLocusKey);

        Call secondCall = getCall(secondLocusKey);
        if (secondCall != null) {
            MediaSession mediaSession = secondCall.getMediaSession();
            if (mediaSession != null) {
                mediaEngine.setActiveMediaSession(mediaSession);
            }
        }
    }


    public void addUsersToCall(Set<Person> people) {

        List<LocusInvitee> invitees = new ArrayList<>();
        for (Person person : people) {
            LocusInvitee locusInvitee = new LocusInvitee(coreFeatures);
            locusInvitee.setInvitee(person.getEmailOrUUID());
            invitees.add(locusInvitee);

        }
        locusService.addUsersToLocus(locusKey, invitees);
    }

    public void endCallAndLeaveLocusSync() {
        locusService.setSharingWhiteboard(false);

        Call call = getCall(locusKey);
        if (call != null && call.getLocusData() != null) {
            LocusData locusData = call.getLocusData();
            callUi.dismissRingback(call);
            stopUnansweredHandler();

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                call.setWasMediaFlowing(mediaSession.wasMediaFlowing());
            }
            call.setCallStarted(false);
            endMediaSession(call, true);

            handleRoomCallEnd();

            ln.i("CallControlService.endCall, post CallControlEndLocusEvent");
            bus.post(new CallControlEndLocusEvent(locusKey));

            // If self where set to a left state remotely, this happens when we leave with observing
            // resource, do not attempt to leave again, as we are already left.
            LocusSelfRepresentation self = locusData.getLocus().getSelf();
            Ln.i("endCall, self state = " + (self != null ? self.getState() : "<not present>"));
            if ((self == null || self.getState() != LocusParticipant.State.LEFT)) {
                locusService.leaveLocusSync(locusKey, locusData.getObservingResource());
            } else {
                ln.i("Not leaving as self is missing or self state != LEFT");
                call.setMediaSession(null);
                call.setActive(false);
            }
        }
    }


    public synchronized void endCall(LocusKey locusKey) {
        ln.i("CallControlService.endCall, locus = " + locusKey);
        endCall(locusKey, false);
    }


    /**
     * End Call
     * @param keepObservingResourceInCall keep the observing resource in call or not if have
     */
    public synchronized void endCall(LocusKey locusKey, boolean keepObservingResourceInCall) {
        ln.i("CallControlService.endCall, locus = %s keepObservingResourceInCall = ? %b", locusKey, keepObservingResourceInCall);
        locusService.setSharingWhiteboard(false);

        Call call = getCall(locusKey);
        if (call != null && call.getLocusData() != null) {
            LocusData locusData = call.getLocusData();
            callUi.dismissRingback(call);
            stopUnansweredHandler();

            MediaSession mediaSession = call.getMediaSession();
            ln.i("CallControlService.endCall, call id = " + call.getCallId() + ", mediaSession = " + mediaSession);
            if (mediaSession != null) {
                call.setWasMediaFlowing(mediaSession.wasMediaFlowing());
            }
            call.setCallStarted(false);
            endMediaSession(call, true);

            handleRoomCallEnd();

            // If self where set to a left state remotely, this happens when we leave with observing
            // resource, do not attempt to leave again, as we are already left.
            LocusSelfRepresentation self = locusData.getLocus().getSelf();
            Ln.i("endCall, self state = " + (self != null ? self.getState() : "<not present>"));
            if ((self == null || self.getState() != LocusParticipant.State.LEFT)) {
                String observingResource = locusData.getObservingResource();
                if (observingResource != null && !keepObservingResourceInCall) {
                    locusService.leaveLocus(locusKey, observingResource);
                } else {
                    locusService.leaveLocus(locusKey);
                }
            } else {
                ln.i("Not leaving as self is missing or self state != LEFT");
                call.setMediaSession(null);
                call.setActive(false);
            }
            ln.i("CallControlService.endCall, post CallControlEndLocusEvent");
            bus.post(new CallControlEndLocusEvent(locusKey));
        }
    }

    public MediaShare getCurrentMediaShare() {
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            MediaShare share = call.getLocus().getWhiteboardMedia();
            return share;
        }
        return null;
    }

    final Object leaveCallSyncLock = new Object();

    /**
     * Leave Call
     * Will hang up on observing TP too if is currently in observing state
     */
    public void leaveCall(LocusKey locusKey) {
        boolean keepObservingResourceInCall = false;
        leaveCall(locusKey, keepObservingResourceInCall);
    }

    /**
     * Leave Call
     * @param locusKey call to leave
     * @param keepObservingResourceInCall if true leaves without bringing down the observing resource
     */
    public void leaveCall(LocusKey locusKey, boolean keepObservingResourceInCall) {
        // TODO we need to move away from CCS having locusKey state and where possible pass it
        // in to all methods (like we do in most cases right now)...this is temporary change in meantime
        this.locusKey = locusKey;
        leaveCall(keepObservingResourceInCall);
    }

    /**
     * Leave Call
     * Will hang up on observing TP too if have
     */
    public void leaveCall() {
        boolean keepObservingResourceInCall = false;
        leaveCall(keepObservingResourceInCall);
    }

    /**
     * Leave Call
     * @param keepObservingResourceInCall keep the observing resource in call or not if have
     */
    public void leaveCall(boolean keepObservingResourceInCall) {
        ln.i("CallControlService.leaveCall, locus = %s keepObservingResourceInCall ? %b", locusKey, keepObservingResourceInCall);
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                // protecting this code path so it is only called by one thread at a time.
                // When you get access verify that self is still JOINED before ending call
                synchronized (leaveCallSyncLock) {

                    Call call = getCall(locusKey);
                    if (call != null && call.getLocusData() != null && call.getLocusData().getLocus() != null && call.getLocusData().getLocus().getSelf() != null) {
                        LocusParticipant.State state = call.getLocusData().getLocus().getSelf().getState();
                        Ln.d("leaveCall, self state = " + state);
                        if (state == LocusParticipant.State.JOINED || call.getLocusData().getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
                            endCall(locusKey, keepObservingResourceInCall); // endCall will set self state to LEAVING

                            // TODO: if we leave the call while the app is in the background - this can throw an exception:
                            // Caused by: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
                            // On the bus.post line.  We need to handle this is another way, so that we can show the dialog to the
                            // user when she arrives back in the app?
                            sendLeaveLocusEvent(call);

                            bus.post(new CallNotificationRemoveEvent(locusKey));
                        } else {
                            ln.d("leaveCall, is already leaving");
                        }
                    }
                }
                return null;
            }
        }.execute();
    }

    /**
     * Leave call when observing a resource
     *
     * @param resource
     * @param postLeave if this is an actual leave, we will fi. show the MOS dialog, otherwise this
     *                  is a copy/move to device, and we only remove the paired device from the call
     */
    public void leaveCall(LocusKey locusKey, String resource, boolean postLeave) {
        ln.i("CallControlService.leaveCall, resource = %s, do postLeave work = %b", resource, postLeave);
        locusService.leaveLocus(locusKey, resource);
        if (postLeave) {
            Call call = getCall(locusKey);
            if (call != null) {
                sendLeaveLocusEvent(call);
            }
        }
    }

    private void reportCallLeaveMetricsAndLogs(Call call) {
        // If 'auto-upload' is on and not on a debug build, then upload our logs to
        // admin service for diagnosis and analysis
        boolean uploadCallLogs = coreFeatures.uploadCallLogs();
        boolean releaseBuild = !BuildConfig.DEBUG;

        if (shouldReportCallLogs(call)) {
            if (!isInCall(call)) {
                call.setCallConnected(false);
            }

            callMetricsReporter.reportLeaveMetrics(call);

            if (uploadCallLogs && releaseBuild) {
                uploadLogsService.uploadLogs(call);
            } else if (!call.wasMediaFlowing() && releaseBuild) {
                // If some media failure occurred and we're not automatically uploading logs,
                // then warn/prompt/'ask to auto-upload' according to setting.
                callUi.requestUserToUploadLogs(call);
            } else {
                Ln.i("Skipping post-call upload.");
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(LocusLeftEvent event) {
        ln.i("CallControlService.onEvent(LocusLeftEvent), locus = " + event.getLocusKey());

        Call call = getCall(event.getLocusKey());
        if (call != null) {
            ln.i("CallControlService.onEvent(LocusLeftEvent), callConnected = " + call.isCallConnected() + ", call id = " + call.getCallId());

            reportCallLeaveMetricsAndLogs(call);

            if (!call.getLocusData().getLocus().isJoinedFromThisDevice(deviceRegistration.getUrl())) {
                call.setActive(false);
            }

            if (coreFeatures.isCallSpinnerEnabled()) {
                setSpinnerHideState(false);
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LocusLeaveFailedEvent event) {
        ln.i("CallControlService.onEvent(LocusLeaveFailedEvent), locus = " + event.getLocusKey());

        Call call = getCall(event.getLocusKey());
        if (call != null) {
            ln.i("CallControlService.onEvent(LocusLeaveFailedEvent), callConnected = " + call.isCallConnected() + ", call id = " + call.getCallId());
            call.setActive(false);
        }
    }

    public void getOrCreateMeetingInfo(@NonNull final LocusKey locusKey,
                                       @NonNull Action<LocusMeetingInfo> successCallback,
                                       @Nullable Action<Exception> failureCallback) {
        locusService.new GetOrCreateMeetingInfoTask(locusKey, successCallback, failureCallback).execute();
    }

    private void logStartMediaStats(Call call) {
        // Log locus ID, locus lastActive timestamp, and trackingID for media troubleshooting
        String callID = call.getLocusData().getLocus().getUniqueCallID();
        if (callID.isEmpty()) {
            Ln.i("Unable to retrieve locusID and locus lastActive.");
            callID = "";
        }
        float timezoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (float) (1000 * 3600); // in hours
        Ln.i("SQMedia Statistics for call ID - %s,%s,%.2f", callID, call.getJoinLocusTrackingID(), timezoneOffset);
    }

    // log the current snapshot of media statistics.
    // this is mostly for end-of-call debugging to help ABC and ABS testing
    private void logEndMediaStats(MediaSession mediaSession) {
        ln.i("Preparing end-of-call media logs");

        MediaStats stats = mediaSession.getStats();
        if (stats == null) {
            ln.i("No media stats available for logging");
            return;
        }

        String statsStr = gson.toJson(stats);
        statsStr = statsStr.substring(1, statsStr.length() - 1);  // remove the first and last brackets

        String locusID = "unknown";
        String locusTimestamp = "unknown";
        LocusData locusData = getLocusData(locusKey);
        if (locusData != null) {
            locusID = locusData.getKey().getLocusId();
            locusTimestamp = DateUtils.formatUTCDateString(locusData.getLocus().getFullState().getLastActive());
        }
        ln.i("=ABC Metrics Report={%s, \"locusTimestamp\": \"%s\", \"locusId\": \"%s\"}=End ABC Metrics Report=", statsStr, locusTimestamp, locusID);
    }

    public enum CancelReason {
        LOCAL_ICE_FAILURE,
        UNANSWERED_TIMEOUT,
        LOCAL_CANCELLED,
        REMOTE_CANCELLED
    }

    /**
     * Cancel a call that hasn't connected yet, with the option to let the observed resource stay in the call
     * @param locusKey
     * @param reason what is the reason for the cancel
     * @param keepObservingResourceInCall whether to let the observed resource remain in the call
     */
    public void cancelCall(final LocusKey locusKey, final CancelReason reason, final boolean keepObservingResourceInCall) {
        ln.i("CallControlService.cancelCall");
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {

                Call call;
                if (locusKey != null) {
                    call = getCall(locusKey);
                } else {
                    call = getActiveCall();
                }
                endMediaSession(call, false);
                String observingResource = call.getLocusData().getObservingResource();
                if (observingResource != null) {
                    // We have an observing resource
                    if (keepObservingResourceInCall) {
                        ln.i("CallControlService.cancelCall leave without observing resource");
                        locusService.leaveLocus(locusKey);
                    } else {
                        ln.i("CallControlService.cancelCall leave with observing resource");
                        locusService.leaveLocus(locusKey, observingResource);
                    }
                } else {
                    // no observing resource
                    ln.i("CallControlService.cancelCall none observing resources");
                    locusService.leaveLocus(locusKey);
                }

                if (call.getLocusData() != null) {
                    call.setCallStarted(false);

                    if (reason == CancelReason.LOCAL_CANCELLED)
                        call.setCallEndReason(new CallEndReason(CANCELLED_BY_LOCAL_USER));
                    else if (reason == CancelReason.LOCAL_ICE_FAILURE)
                        call.setCallEndReason(new CallEndReason(CANCELLED_BY_LOCAL_ERROR, "ICE connection failure"));

                    logCall(LogCallIndex.CANCELED, call.getLocusData());

                    callUi.dismissRingback(call);
                    stopUnansweredHandler();

                    reportCallCancelledMetrics(call);

                    bus.post(new CallControlCallCancelledEvent(locusKey, reason));
                    bus.post(new CallNotificationRemoveEvent(locusKey));
                }
                return null;
            }
        }.execute();

    }

    /**
     * Cancel Call
     * Default behavior is to let the observed resource stay in the call
     */
    public synchronized void cancelCall(LocusKey locusKey, CancelReason cancelReason) {
        cancelCall(locusKey, cancelReason, true);
    }

    /**
     * Decline Call
     */
    public void declineCall(LocusKey locusKey) {
        ln.i("CallControlService.declineCall");

        LocusData locusData = getLocusData(locusKey);
        if (locusData != null) {
            locusService.declineLocus(locusKey, DeclineReason.UNKNOWN);
            callNotification.dismiss(locusKey);
            bus.post(new CallNotificationRemoveEvent(locusKey));
            logCall(LogCallIndex.DECLINED, locusData);
        }
    }

    /**
     * Ignore Call
     */
    public void ignoreCall(LocusKey call) {
        ln.i("CallControlService.ignoreCall - dismiss sound/vibrate and update notification");
        callNotification.dismiss(call);
        bus.post(new CallNotificationUpdateEvent(CallNotificationType.IGNORED, locusKey, false));
    }


    public MediaConnection getMediaConnection(Locus locus) {
        if (locus != null) {
            LocusParticipant self = locus.getSelf();
            if (self != null) {
                for (LocusParticipantDevice device : self.getDevices()) {
                    if (device.getUrl().equals(deviceRegistration.getUrl()) && device.getMediaConnections() != null) {
                        return device.getMediaConnections().get(0);
                    }
                }
            }
        }
        return null;
    }


    public void muteAudio(LocusKey locusKey) {
        // Mute/un-mute audio in the local media session. (Call WME API to mute local device)
        muteAudioInMediaSession(locusKey, true);

        // signal audio mute status to the locus.
        // Call Locus API /locus/api/v1/loci/{lid}/participant/{pid}/media
        // to notify server local-mute action happened on this device.
        modifyMedia(locusKey, AUDIO_TYPE, true);

        audioMutedLocally = true;
        callAnalyzerReporter.reportMuted(getCall(locusKey), MediaType.AUDIO);
        bus.post(new CallControlLocalAudioMutedEvent(locusKey, true));
        bus.post(new CallNotificationUpdateEvent(CallNotificationType.MUTE_STATE, locusKey, true));
    }

    public void unmuteAudio(LocusKey locusKey) {
        // Un-mute audio in the local media session. (Call WME API to unmute local device)
        boolean localMuted  = isAudioMuted(locusKey); // Indication of a local mute
        muteAudioInMediaSession(locusKey, false);
        audioMutedLocally = false;

        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call == null) {
            return;
        }

        Locus locus = call.getLocus();
        if (locus == null) {
            return;
        }

        LocusParticipant self = locus.getSelf();
        if (self == null) {
            return;
        }

        // Check both ways of indicating a mute status of a participant
        LocusParticipantControls controls = self.getControls();
        boolean remoteMuted = (controls != null) && (controls.getAudio() != null) && controls.getAudio().isMuted(); // Indication of a remote (bridge) mute.

        // If self was remote-muted, do remote un-mute.
        // Reference: Case B-b in https://wiki.cisco.com/pages/viewpage.action?pageId=60770064
        if (remoteMuted) {
            locusService.modifyParticipantControls(locusKey, self, false);
        }

        // If self was local-muted, do local unmute.
        // Reference: Case B-a in https://wiki.cisco.com/pages/viewpage.action?pageId=60770064
        if (localMuted) {
            modifyMedia(locusKey, AUDIO_TYPE, false);
        }

        callAnalyzerReporter.reportUnmuted(getCall(locusKey), MediaType.AUDIO);

        bus.post(new CallControlLocalAudioMutedEvent(locusKey, false));
        bus.post(new CallNotificationUpdateEvent(CallNotificationType.MUTE_STATE, locusKey, false));
    }

    public boolean isAudioMuted(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            return mediaSession.isAudioMuted();
        }
        return false;
    }

    public boolean isConversationInCall(String convUuid) {

        if (!locusDataCache.isInCall()) {
            return false;
        }

        LocusKey locusKey = locusDataCache.getActiveLocus();
        LocusData locusData = locusDataCache.getLocusData(locusKey);

        if (locusData == null || locusData.getLocus() == null) {
            return false;
        }

        String locusConvId = getIdFromUrl(locusData.getLocus().getConversationUrl());
        return locusConvId != null && locusConvId.equals(convUuid);
    }

    public void muteRemoteAudio(LocusKey locusKey, boolean muted) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            if (muted) {
                mediaSession.muteRemoteAudio();
                locusService.callFlowTrace("App", "WME", "muteRemoteAudio()", locusKey);
            } else {
                mediaSession.unMuteRemoteAudio();
                locusService.callFlowTrace("App", "WME", "unMuteRemoteAudio()", locusKey);
            }
        }
    }

    //sdk
    public void muteRemoteVideo(LocusKey locusKey, boolean muted){
        Log.i("CallControlService", "muteRemoteVideo: ->start");
        Ln.i("muteRemoteVideo: ->start");

        //sdk modification

        String sdk = BuildConfig.PUBLICSDK;

        if(sdk.equals("SDKEnabled")){
            Ln.i("SDKEnabled");

            MediaSession mediaSession = getMediaSession(locusKey);
            if (mediaSession != null) {
                if (muted) {
                    Log.i("CallControlService", "not receiving");
                    mediaSession.muteRemoteVideo();

                } else {
                    Log.i("CallControlService", "receiving");
                    mediaSession.unmuteRemoteVideo();

                }
            }
            return;
        }else{
            Log.i("CallControlService", "not in SDK enabled");
            return;
        }

    }

    /**
     * Mute audio in the local media session as a result of mute action from another user (remote muting)
     * This would also result in modify media local API call.
     */
    public void muteAudioFromRemote(LocusKey locusKey, boolean muted) {
        // In multidevice scenario, if one device is in call and other device with the same user has
        // not joined the call, do nothing for that device.
        Call call = getCall(locusKey);
        if (call != null && !isInCall(call)) {
            return;
        }
        // If the local audio mute status is not different from the indicated status, do nothing.
        boolean audioSessionMuted = isAudioMuted(locusKey);
        if (audioSessionMuted == muted) {
            return;
        }

        // If audio is muted by self (locally) then a mute/unmute request from the remote should be ignored.
        // Such a request can come when self is logged in from multiple devices.
        // Self could be muted locally - however, self in a different device need not be muted.
        // Then a different user could remote-mute and remote-unmute on self overriding the self muted action.
        if (audioMutedLocally && audioSessionMuted && !muted) {
            return;
        }

        // Mute audio in the local media session. (Call WME API to mute local device)
        muteAudioInMediaSession(locusKey, muted);

        bus.post(new CallNotificationUpdateEvent(CallNotificationType.MUTE_STATE, locusKey, muted));
    }

    public void muteVideo(LocusKey locusKey, MediaRequestSource source) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            final boolean videoMuted = mediaSession.isVideoMuted();
            if (!videoMuted) {
                mediaSession.muteVideo(source);
                locusService.callFlowTrace("App", "WME", "sendVideo(false)", locusKey);
                // signal video mute status
                modifyMedia(locusKey, VIDEO_TYPE, true);
                callAnalyzerReporter.reportMuted(getCall(locusKey), MediaType.VIDEO);
                bus.post(new CallControlLocalVideoMutedEvent(locusKey, true, source));
            }
        }
    }


    public boolean isVideoMuted(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            return mediaSession.isVideoMuted();
        }
        return false;
    }

    public boolean unMuteVideo(LocusKey locusKey, MediaRequestSource source) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            final boolean videoMuted = mediaSession.isVideoMuted();
            final MediaRequestSource videoWasMutedBy = mediaSession.getVideoMuteSource();
            // Ignore if unmute source is PROXIMITY and the videoWasMuted by another source.
            final boolean ignore;
            if (((videoWasMutedBy != MediaRequestSource.PROXIMITY) && (source == MediaRequestSource.PROXIMITY))) {
                ignore = true;
            } else {
                ignore = false;
            }

            if (videoMuted && !ignore) {
                mediaSession.unMuteVideo();
                locusService.callFlowTrace("App", "WME", "sendVideo(true)", locusKey);

                // signal video mute status
                modifyMedia(locusKey, VIDEO_TYPE, false);
                callAnalyzerReporter.reportUnmuted(getCall(locusKey), MediaType.VIDEO);
                bus.post(new CallControlLocalVideoMutedEvent(locusKey, false, source));
                return true;
            } else {
                Ln.w("unmute failed or ignored: isMuted=%s, getVideoMuteSource=%s, from source=%s", videoMuted,
                        videoWasMutedBy.toString(), source.toString());
                return false;
            }
        }
        return false;
    }

    public boolean isMeetingLocked(LocusKey locusKey) {
        LocusData locusData = getLocusData(locusKey);

        if (locusData == null)
            return false;

        Locus locus = locusData.getLocus();

        if (locus == null || locus.getControls() == null)
            return false;

        return locus.getControls().getLock().isLocked();
    }

    public boolean isAbleToRecordMeeting(LocusKey locusKey) {
        Locus locus = getLocus(locusKey);
        LocusRecordControl recordControl = locus != null ? locus.getRecordControl() : null;
        return recordControl != null && recordControl.isAbleToRecord() && isModerator(locusKey);
    }

    public boolean isMeetingRecording(LocusKey locusKey) {
        Locus locus = getLocus(locusKey);
        LocusRecordControl recordControl = locus != null ? locus.getRecordControl() : null;
        return recordControl != null && recordControl.isRecording();
    }

    public boolean isMeetingRecordingPaused(LocusKey locusKey) {
        Locus locus = getLocus(locusKey);
        LocusRecordControl recordControl = locus != null ? locus.getRecordControl() : null;
        return recordControl != null && recordControl.isPaused();
    }

    public boolean isModerator(LocusKey locusKey) {
        Locus locus = getLocus(locusKey);
        return locus != null && locus.getSelf() != null && locus.getSelf().isModerator();
    }

    public void muteAudioInMediaSession(@NonNull LocusKey locusKey, boolean muted) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession == null) {
            return;
        }

        if (muted) {
            mediaSession.muteAudio();
            locusService.callFlowTrace("App", "WME", "muteAudioInMediaSession()", locusKey);
        } else {
            mediaSession.unMuteAudio();
            locusService.callFlowTrace("App", "WME", "unmuteAudioInMediaSession()", locusKey);
        }
    }

    private void modifyMedia(LocusKey locusKey, final String mediaType, final boolean muted) {

        Call call = getCall(locusKey);
        if (call != null && call.getLocusData() != null) {
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                String currentLocalSdp = mediaSession.getLocalSdp();
                String calliopeSupplementaryInformationString = buildCalliopeSupplementaryInformation();
                Map<String, Object> clusterInfo = linusReachabilityService.getLatestLinusReachabilityResults();
                Ln.d("modifyMedia, clusterInfo is " + clusterInfo);
                MediaInfo mediaInfo = new MediaInfo(currentLocalSdp, MediaEngine.SDP_TYPE, calliopeSupplementaryInformationString, clusterInfo);

                if (mediaType.equals(AUDIO_TYPE)) {
                    mediaInfo.setAudioMuted(muted);
                    mediaInfo.setVideoMuted(mediaSession.isVideoMuted());
                } else {
                    mediaInfo.setVideoMuted(muted);
                    mediaInfo.setAudioMuted(mediaSession.isAudioMuted());
                }

                MediaConnection currentMediaConnection = getMediaConnection(call.getLocusData().getLocus());
                if (currentMediaConnection != null) {
                    List<MediaConnection> mediaConnectionList = new ArrayList<MediaConnection>();
                    MediaConnection mediaConnection = new MediaConnection();
                    mediaConnection.setType("SDP");
                    mediaConnection.setMediaId(currentMediaConnection.getMediaId());
                    mediaConnection.setLocalSdp(gson.toJson(mediaInfo));
                    mediaConnectionList.add(mediaConnection);

                    locusService.modifyMedia(locusKey, mediaConnectionList, mediaSession.isAudioMuted());
                }
            }
        }
    }

    public void enableDtmfReceive(boolean value) {
        dtmfReceiveSupported = value;
    }

    public Response sendDtmf(final int correlationId, final String tones) {
        if (locusDataCache.isInCall(locusKey)) {
            return locusService.sendDtmf(locusKey, correlationId, tones);
        }

        return null;
    }

    private void startUnansweredHandler() {
        if (unansweredHandler == null) {
            unansweredHandler = new Handler(context.getMainLooper());
            ln.d("start unanswered handler, timeout: %s", callUi.getRingbackTimeout());
            unansweredHandler.postDelayed(cancelCall, TimeUnit.SECONDS.toMillis(callUi.getRingbackTimeout()));
        }
    }

    private void stopUnansweredHandler() {
        ln.d("stopUnansweredHandler");
        if (unansweredHandler != null) {
            unansweredHandler.removeCallbacks(cancelCall);
            unansweredHandler = null;
        }
    }

    Runnable cancelCall = new Runnable() {
        @Override
        public void run() {
            final Call call = getCall(locusKey);
            if (call != null) {
                ln.d("Unanswered call timeout occured (timeout %d secs)", callUi.getRingbackTimeout());
                Lns.ux().i("Unanswered timeout occurred (after %d secs)", callUi.getRingbackTimeout());

                toaster.showLong(context, context.getString(R.string.name_was_unavailable, NameUtils.getFirstName(call.getLocusData().getRemoteParticipantName())));
                bus.post(new CallControlCallCancelledEvent(locusKey, CancelReason.UNANSWERED_TIMEOUT));

                new SafeAsyncTask<Void>() {
                    @Override
                    public Void call() throws Exception {
                        call.setCallEndReason(new CallEndReason(DIAL_TIMEOUT_REACHED));
                        endCall(locusKey);
                        return null;
                    }
                }.execute();
            }
        }
    };


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ParticipantDeclinedEvent event) {
        Ln.i("CallControlService.onEvent(ParticipantDeclinedEvent)");

        Call call = getCall(event.getLocusKey());
        if (call != null && call.getLocusData() != null) {
            LocusData locusData = call.getLocusData();
            Locus locus = locusData.getLocus();

            // Only end call when we get a decline if we're in 1:1 and the other person has declined
            if (locusData.onlyMeJoined() && !locusData.isBridge()) {
                call.setCallStarted(false);
                call.setCallEndReason(new CallEndReason(DECLINED_BY_REMOTE_USER));

                callUi.dismissRingback(call);
                stopUnansweredHandler();

                endMediaSession(call, false);

                // for 1:1 joinedCalls, show message that other person has declined
                if (event.getReason() != null && event.getReason().equals(DeclineReason.BUSY)) {
                    toaster.showLong(context, R.string.call_other_person_busy);
                    logCall(LogCallIndex.BUSY, call.getLocusData());
                } else {
                    toaster.showLong(context, R.string.call_other_person_declined);
                    logCall(LogCallIndex.DECLINED, call.getLocusData());
                }

                locusService.leaveLocus(event.getLocusKey());
                bus.post(CallControlLeaveLocusEvent.callDeclined(locusData));
            }
        }
    }

    public void copyCall(LocusKey locusKey) {
        Ln.i("CallControlService.copyCall, locusKey = " + locusKey);

        Call call = getCall(locusKey);
        if (call == null) {
            call = getActiveCall();
        }

        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();
            Ln.i("CallControlService.copyCall, call id = " + call.getCallId() + ", media session = " + mediaSession);

            // end current media session, for new start in join
            endMediaSession(call, false);
            call.setActive(false);


            RoomUpdatedEvent.RoomState roomState;
            boolean isObserving = isInPairedCall(call);

            // if in observing state then media is on room endpoint and we're copying/moving to local device
            if (isObserving) {
                Ln.i("Copying call from room to local device");
                roomState = RoomUpdatedEvent.RoomState.PAIRED;

                // join call with media to this device
                CallContext callContext = new CallContext.Builder(locusKey)
                        .setUseRoomPreference(CallContext.UseRoomPreference.DontUseRoom)
                        .setPromptLeaveRoom(true)
                        .setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioVideoShare)
                        .setShowFullScreen(true).build();
                call = joinCall(callContext);

                // start copied call in muted state
                mediaSession = call.getMediaSession();
                if (mediaSession != null) {
                    mediaSession.muteAudio();
                    mediaSession.muteRemoteAudio();
                }

            } else {
                Ln.i("Moving call to room endpoint.");
                roomState = RoomUpdatedEvent.RoomState.CONNECTED;

                // move call to room endpoint
                CallContext callContext = new CallContext.Builder(locusKey)
                        .setMoveMediaToResource(true)
                        .setMediaDirection(MediaEngine.MediaDirection.SendReceiveShareOnly)
                        .setIsOneOnOne(call.getLocusData().isOneOnOne())
                        .setShowFullScreen(true).build();
                joinCall(callContext);
            }

            isObserving = !isObserving;
            boolean localMedia = !isObserving;
            ln.i("CallControlService.copyCall: isObserving=%b, room state=%s, local media=%b", isObserving, roomState.toString(), localMedia);

            updateRoomCopyCall(isObserving, roomState, localMedia);

            bus.post(new CallNotificationUpdateEvent(CallNotificationType.MEDIA_STATE, locusKey, localMedia));
        }
    }

    private void logCallInfo(String methodTag, Locus locus) {
        int numberParticipants = locus.getFullState().getCount();
        String selfState = locus.getSelf() != null ? locus.getSelf().getState().toString() : "";
        ln.i("%s, joined participant count = %d, state = %s", methodTag, numberParticipants, selfState);
        for (LocusParticipant participant : locus.getParticipants()) {
            if (participant.getState().equals(LocusParticipant.State.JOINED)) {
                ln.v("%s,   %s", methodTag, participant.getPerson().getDisplayName());
            }
        }

        LocusParticipant.Intent intent = locus.getIntent(deviceRegistration.getUrl());
        if (intent != null) {
            ln.i("%s, intent type = %s, associated with = %s", methodTag, intent.getType(), intent.getAssociatedWith());
        }
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(ParticipantJoinedEvent event) {
        synchronized (syncLock) {
            String methodTag = "CallControlService.onEvent(ParticipantJoinedEvent)";
            logCallInfo(methodTag, event.getLocus());

            dumpJoinedCalls();


            // The LocusKey from the event should be used for work within this method, but it should NOT update this.locusKey yet!
            LocusKey localLocusKey = event.getLocusKey();
            Call call = getCall(event.getLocusKey());
            if (call != null && call.getLocusData() != null) {
                LocusData locusData = call.getLocusData();
                Locus locus = locusData.getLocus();
                int numberJoinedParticipants = locus.getFullState().getCount();

                if (locusDataCache.isInCall(localLocusKey)) { // only handle this event if we're actually joined on this device

                    callNotification.dismiss(localLocusKey);

                    // check if we're in observing state (joined but with media on room endpoint)
                    boolean isObserving = locusData.isObserving(deviceRegistration.getUrl());
                    Ln.i("%s, joined on this device, observing = %b, mediaStarted = %b", methodTag, isObserving, isMediaStarted(locusKey));

                    if (!isObserving) {
                        // if not in observing state then start local media
                        if (!isMediaStarted(locusKey)) {
                            startMedia(call);
                            locusData.setActiveSpeakerId(null);
                        }
                    } else {
                        if (!isMediaStarted(locusKey)) {
                            startMedia(call);
                            setRoomJoined();
                        }
                    }

                    if (!call.isCallStarted()) {
                        call.setCallStarted(true);
                        bus.post(new CallControlCallStartedEvent(localLocusKey));

                        if (isObserving) {
                            setRoomCallNotifying();
                        }
                    }

                    // check if we're now "connected" i.e. other participant(s) now joined (note that room counts as participant
                    // when we're in observing data). Alternatively we could be in the lobby of the webex meeting waiting for the host
                    // and need to check for that
                    int callConnectParticipantCount = locusData.isObservingNotMoving(deviceRegistration.getUrl()) ? 3 : 2;
                    if (numberJoinedParticipants >= callConnectParticipantCount || locusData.getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {

                        // callConnected flag is only used when we have media on local device (i.e.
                        if (!isObserving && !call.isCallConnected()) {

                            call.setCallConnected(true);
                            call.setEpochStartTime(new Date().getTime());
                            bus.post(new CallControlCallConnectedEvent(localLocusKey));

                            handleMediaDeviceRegistration();

                            callMetricsReporter.reportJoinMetrics(call);
                            logCall(LogCallIndex.CONNECTED, call.getLocusData());
                        }


                        if (isObserving) {
                            String remoteParticipantName = locusData.getRemoteParticipantName();
                            // TODO check this logic....is here to handle case where calling sip endpoint....can we check for no covnersation url?
                            String conversationTitle = locus.getParticipants().size() > 3 ? null : remoteParticipantName; // passing null will keep the original name of the room.
                            setRoomCallConnected(conversationTitle);
                            logCall(LogCallIndex.CONNECTED, call.getLocusData());
                        }


                        callUi.dismissRingback(call);
                        stopUnansweredHandler();

                        // Make sure whiteboard share flag is correct due to Floor event can't be received when call is left in current design.
                        // Need to work with Server team to see if they can send this event in this case
                        if (locusData.isFloorGranted()) {
                            if (locus.getWhiteboardMedia() != null) {
                                locusService.setSharingWhiteboard(true);
                            } else {
                                locusService.setSharingWhiteboard(false);
                            }
                        }
                        bus.post(new CallControlParticipantJoinedEvent(localLocusKey, event.getJoinedParticipants()));
                        bus.post(new CallNotificationEvent(CallNotificationType.ONGOING, localLocusKey, locusData.isOneOnOne(), isMediaStarted(locusKey)));
                    } else {
                        if (locusData.isOneOnOne() && !locusData.isMeeting() && !locusData.getLocus().isJoinedFromOtherDevice(deviceRegistration.getUrl())) {
                            callUi.startRingback(call);
                            startUnansweredHandler();
                        }

                        bus.post(new CallControlLocusCreatedEvent(localLocusKey));

                        sendCallNotificationEvent(localLocusKey, locusData);
                    }
                } else {
                    Ln.i("%s, not joined on this device - ignore", methodTag);
                }
            } else {
                Ln.i("%s, no call found for key", methodTag);
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ParticipantLeftEvent event) {
        // To be able to override event handler in subclass
        handleEventParticipantLeft(event);
    }

    protected void handleEventParticipantLeft(ParticipantLeftEvent event) {
        String methodTag = "CallControlService.onEvent(ParticipantLeftEvent)";
        logCallInfo(methodTag, event.getLocus());

        dumpJoinedCalls();

        // Use LocusData rather than Call because in ParticipantLeftEvent we may not have an active Call
        //  1. if we are yet to join the call, and the far end 'cancels'
        //  2. if we are the one leaving - the Call object will have already been set to inactive in CallControlSelfParticipantLeftEvent
        LocusData locusData = locusDataCache.getLocusData(event.getLocusKey());
        if (locusData != null) {
            Locus locus = locusData.getLocus();
            int remainingParticipants = locus.getFullState().getCount();

            // check if this is indication that user cancelled call
            if (locusData.isOneOnOne() && remainingParticipants == 0 && locus.getSelf() != null && locus.getSelf().getState().equals(LocusParticipant.State.NOTIFIED)) {
                toaster.showLong(context, R.string.call_participant_cancelled);
                bus.post(new CallControlCallCancelledEvent(locusData.getKey(), CancelReason.REMOTE_CANCELLED));
            } else {
                Call call = getCall(event.getLocusKey());
                if (call != null && isInCall(call)) {
                    int self = locusData.isObserving(deviceRegistration.getUrl()) ? 2 : 1;
                    if (locusData.isOneOnOne() && remainingParticipants == self && !locusData.isMeeting()) {
                        // One-on-one and we are still in the call
                        toaster.showLong(context, R.string.call_participant_left);
                        call.setCallEndReason(new CallEndReason(ENDED_BY_REMOTE_USER));
                        leaveCall(event.getLocusKey());
                    } else {
                        ln.d("skip leaveCall() because: isOneOnOne=%b, remainingParticipants=%d, self=%d, !isMeeting=%b",
                                locusData.isOneOnOne(), remainingParticipants, self, !locusData.isMeeting());
                    }
                }
                bus.post(new CallControlParticipantLeftEvent(locusData.getKey(), event.getLeftParticipants()));
            }

        } else {
            ln.i("%s, no locus data found for key", methodTag);
        }
    }

    private boolean isInCall(Call call) {
        return locusDataCache.isInCall(call.getLocusData().getKey());
    }

    protected boolean isInPairedCall(Call call) {
        if (call == null)
            return false;

        LocusData ld = call.getLocusData();
        if (ld == null)
            return false;
        return ld.isObserving(deviceRegistration.getUrl());
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(AnsweredInactiveCallEvent event) {
        Ln.i("CallControlService.onEvent(AnsweredInactiveCallEvent)");
        Call call = getCall(event.getLocusKey());
        if (call != null) {
            endMediaSession(call, false);
            locusService.leaveLocus(event.getLocusKey());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(ParticipantChangedEvent event) {
        ln.i("CallControlService.onEvent(ParticipantChangedEvent)");
        bus.post(new CallControlParticipantChangedEvent(event.getLocusKey()));
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(ParticipantSelfChangedEvent event) {
        // To be able to override event handler in subclass
        handleEventParticipantSelfChanged(event);
    }

    protected void handleEventParticipantSelfChanged(ParticipantSelfChangedEvent event) {
        ln.i("CallControlService.onEvent(ParticipantSelfChangedEvent)");
        updateLocus(event.getLocusKey(), event.getLocus());
    }

    protected LocusData updateLocus(LocusKey locusKey, Locus locus) {
        LocusData call = locusDataCache.updateLocus(locusKey, locus);
        return call;
    }

    public void modifyLocusControls(LocusKey locusKey, LocusLink locusLink) {
        locusService.modifyLocusControls(locusKey, locusLink);
    }

    public void modifyLocusControls(LocusKey locusKey,  boolean meetingLockState) {
        locusService.modifyLocusControls(locusKey, meetingLockState);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(CallControlSelfParticipantLeftEvent event) {
        ln.i("CallControlService.onEvent(CallControlSelfParticipantLeftEvent)");

        Call call = getCall(event.getLocusKey());
        if (call != null && call.getLocusData() != null) {
            // If !call.isCallStarted then this occurs as a result of us leaving the call
            // by hitting the /leave Locus endpoint
            // If call.isCallStarted then it is due to Locus 'booting us out' the call
            if (call.isCallStarted()) {
                call.setCallEndReason(new CallEndReason(ENDED_BY_LOCUS));
                // Important: this won't hit the Locus server /leave because we are already LEFT
                // Therefore, LocusLeftEvent won't fire, so anything in that handler won't execute
                endCall(event.getLocusKey());

                // Do some of the CallLeftEvent handler here. Should it all just be moved though?
                reportCallLeaveMetricsAndLogs(call);
            }

            if (call.getLocusData().getLocus().getSelf().getReason() == LocusParticipant.Reason.FORCED) {
                toaster.showLong(context, R.string.call_participant_expelled);
            }

            if (call.getLocusData().getLocus().getSelf().getReason() == LocusParticipant.Reason.FORCED) {
                toaster.showLong(context, R.string.call_participant_expelled);
                bus.post(new CallControlMeetingControlsExpelEvent(event.getLocusKey(), true));
            }
        }
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(CallControlHeldEvent event) {
        ln.i("CallControlService.onEvent(CallControlHeldEvent)");
        LocusData call = getLocusData(event.getLocusKey());
        if (call != null) {
            callUi.showMessage(R.string.call_put_on_hold);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(CallControlResumedEvent event) {
        ln.i("CallControlService.onEvent(CallControlResumedEvent)");
        LocusData call = getLocusData(event.getLocusKey());
        if (call != null) {
            callUi.showMessage(R.string.call_resumed);
        }
    }

    private void startMedia(Call call) {
        MediaConnection mediaConnection = getMediaConnection(call.getLocusData().getLocus());
        if (mediaConnection != null) {
            String remoteSdpString = mediaConnection.getRemoteSdp();
            MediaInfo remoteSdp = gson.fromJson(remoteSdpString, MediaInfo.class);

            // remote sdp will only be included in locus DTO if we're doing legacy sdp offer/answer.  For
            // new ROAP flows, the Answer SDP will be included in it's own mercury event
            if (remoteSdp.getSdp() != null) {

                MediaSession mediaSession = call.getMediaSession();
                if (mediaSession != null) {
                    mediaSession.startMedia();

                    String sdp = remoteSdp.getSdp();
                    if (sdp != null && !sdp.isEmpty()) {

                        // check if wme feature toggles are present in self device info
                        Map<String, String> featureToggles = null;
                        LocusSelfRepresentation self = call.getLocusData().getLocus().getSelf();
                        if (self.getDevices().size() > 0) {
                            featureToggles = self.getDevices().get(0).getFeatureToggles();
                        }

                        mediaSession.answerReceived(sdp, featureToggles);

                        // No discernable difference with these two as far as I am aware
                        // Perhaps the latter report was intended for something like WME's onMediaReady?
                        // TODO: Ask someone who knows...
                        callAnalyzerReporter.reportRemoteSdpReceived(call);
                        callAnalyzerReporter.reportMediaEngineReady(call);

                        // This is getting a bit silly now but I guess we can assume that ICE is started
                        // upon receipt of the remote SDP?
                        callAnalyzerReporter.reportIceStart(call);
                    }
                    logStartMediaStats(call);
                }
            }
        }
    }


    private void updateMedia(Call call) {
        MediaConnection mediaConnection = getMediaConnection(call.getLocusData().getLocus());
        if (mediaConnection != null) {
            String remoteSdpString = mediaConnection.getRemoteSdp();
            MediaInfo remoteSdp = gson.fromJson(remoteSdpString, MediaInfo.class);

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {

                String sdp = remoteSdp.getSdp();
                if (sdp != null && !sdp.isEmpty()) {
                    mediaSession.updateSDP(sdp);
                }
            }
        }
    }


    private void logCall(String msg, LocusData locusData) {
        logFilePrint.getCallIndex().addCall(msg, locusData, trackingIdGenerator.currentTrackingId());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(IncomingCallEvent event) {
        Ln.i("CallControlService.onEventMainThread(IncomingCallEvent)");

        locusService.alertLocus(event.getLocusKey());

        Action<LocusKey> answerAction = new Action<LocusKey>() {
            @Override
            public void call(final LocusKey locusKey) {
                new SafeAsyncTask<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Ln.i("answerAction, in AsyncTask");
                        if (locusDataCache.isInCall()) {
                            endCallAndLeaveLocusSync();
                        }
                        LocusData call = getLocusData(locusKey);
                        if (call != null) {
                            CallContext callContext = new CallContext.Builder(locusKey)
                                    .setIsOneOnOne(call.isOneOnOne())
                                    .setIsAnsweringCall(true)
                                    .setShowFullScreen(true)
                                    .setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioVideoShare)
                                    .setUseRoomPreference(CallContext.UseRoomPreference.DontUseRoom)
                                    .setCallInitiationOrigin(CallInitiationOrigin.CallOriginationToast).build();
                            joinCall(callContext);
                        }
                        return null;
                    }
                }.execute();
            }
        };

        Action<LocusKey> declineAction = new Action<LocusKey>() {
            @Override
            public void call(LocusKey locusKey) {
                declineCall(locusKey);
            }
        };

        Action<LocusKey> pairedAction = new Action<LocusKey>() {
            @Override
            public void call(final LocusKey locusKey) {
                new SafeAsyncTask<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Ln.i("pairedAction, in AsyncTask");
                        if (locusDataCache.isInCall()) {
                            endCallAndLeaveLocusSync();
                        }
                        LocusData call = getLocusData(locusKey);
                        if (call != null) {
                            CallContext callContext = new CallContext.Builder(locusKey)
                                    .setIsOneOnOne(call.isOneOnOne())
                                    .setIsAnsweringCall(true)
                                    .setShowFullScreen(true)
                                    .setMediaDirection(MediaEngine.MediaDirection.SendReceiveShareOnly)
                                    .setUseRoomPreference(CallContext.UseRoomPreference.UseRoom)
                                    .setCallInitiationOrigin(CallInitiationOrigin.CallOriginationToast).build();
                            joinCall(callContext);
                        }
                        return null;
                    }
                }.execute();
            }
        };

        Action<LocusKey> timeoutAction = new Action<LocusKey>() {
            @Override
            public void call(LocusKey locusKey) {
                if (getLocusData(locusKey) != null && getLocusData(locusKey).isOneOnOne())
                    bus.post(new CallNotificationEvent(CallNotificationType.MISSED, locusKey, true, true));
            }
        };

        callNotification.notify(event.getLocusKey(), new NotificationActions(answerAction, declineAction, pairedAction, timeoutAction));

        LocusData call = locusDataCache.getLocusData(event.getLocusKey());
        if (call != null) {
            bus.post(new CallNotificationEvent(CallNotificationType.INCOMING, event.getLocusKey(), call.isOneOnOne(), true)); // Answer call from notification is always with local media (=true)
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(MediaUpdatedEvent event) {
        ln.i("CallControlService.onEventMainThread(MediaUpdatedEvent)");
        // Only handle this event when media hasn't been initialized for the call yet.
        Call call = getCall(event.getLocusKey());
        if (call != null) {
            ln.d("CallControlService.onEventMainThread(MediaUpdatedEvent) mediaSessionInEndingState ? %s isMediaStarted ? %s", mediaSessionInEndingState(locusKey), isMediaStarted(locusKey));
            if (!mediaSessionInEndingState(call.getLocusKey())) {
                if (!isMediaStarted(call.getLocusKey())) {
                    ln.d("CallControlService.onEventMainThread(MediaUpdatedEvent) self state = %s %s", call.getLocusData().getLocus().getSelf().getState(), MediaSessionUtils.toString(getMediaSession(locusKey)));
                    startMedia(call);
                } else {
                    updateMedia(call);
                }
            }
        } else {
            ln.w("Could not find Call data key: %s", event.getLocusKey());
        }
    }

    private boolean mediaSessionInEndingState(LocusKey locusKey) {
        final MediaSession mediaSession = getMediaSession(locusKey);
        final boolean mediaSessionCreated = mediaSession != null;
        return  mediaSessionCreated && mediaSession.isMediaSessionEnding();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(MediaCreatedEvent event) {
        ln.i("CallControlService.onEventMainThread(MediaCreatedEvent)");

        /* Only handle this event when media hasn't been initialized for the call yet. */
        Call call = getCall(event.getLocusKey());
        if (call != null && !isMediaStarted(event.getLocusKey()))
            startMedia(call);
        else
            ln.w("Could not find Locus DTO for key: %s", event.getLocusKey());
    }

    /**
     * Notification from media engine that network congestion has been detected.  In response
     * we disable sending video and instruct other endpoint to do likewise
     */
    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(NetworkCongestionEvent event) {
        Ln.i("CallControlService.onEvent(NetworkCongestionEvent)");
        toaster.showLong(context, R.string.poor_network_quality);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(NetworkDisableVideoEvent event) {
        Ln.i("CallControlService.onEvent(NetworkDisableVideoEvent)");

        Call call = getCall(event.getCallId());
        if (call != null) {

            toaster.showLong(context, R.string.video_disabled_due_to_network_conditions);
            Ln.w("Disable sending video due to poor network conditions");
            call.setVideoDisabled(true);

            muteVideo(call.getLocusKey(), MediaRequestSource.NETWORK);

            // notify UI that video has been disabled
            bus.post(new CallControlDisableVideoEvent());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkDisconnectEvent event) {
        Ln.i("CallControlService.onEvent(NetworkDisconnectEvent)");
        Call call = getCall(event.getCallId());
        String disconnectedMedia = event.getMediaType();
        if (call != null && disconnectedMedia.equalsIgnoreCase(AUDIO_TYPE)) {
            locusService.callFlowTrace("App", "WME", "NetworkDisconnectEvent: audio was disconnected, so pause call", call.getLocusKey());
            bus.post(new CallControlDisconnectedEvent());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkReconnectEvent event) {
        Ln.i("CallControlService.onEvent(NetworkReconnectEvent)");
        Call call = getCall(event.getCallId());
        if (call != null) {
            locusService.callFlowTrace("App", "WME", "NetworkReconnectEvent: resume call", call.getLocusKey());
            bus.post(new CallControlReconnectEvent());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkLostEvent event) {
        Ln.i("CallControlService.onEvent(NetworkLostEvent)");
        Call call = getCall(event.getCallId());
        if (call != null) {
            locusService.callFlowTrace("App", "WME", "NetworkLostEvent: end call", call.getLocusKey());
            bus.post(new CallControlLostEvent());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(DeviceCameraUnavailable event) {
        Ln.w("CallControlService.onEvent(DeviceCameraUnavailable)");
        Call call = getCall(event.getCallId());
        if (call != null) {
            toaster.showLong(context, R.string.camera_unavailable);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(StunTraceResultEvent event) {
        Ln.w("CallControlService.onEvent(StunTraceResultEvent)");

        Call call = getCall(locusKey);
        if (call != null) {
            if (call != null && call.getLocusData() != null) {
                callMetricsReporter.reportCallStunTraceMetrics(call, event.getDetail());
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ErrorJoiningLocusEvent event) {
        Ln.i("CallControlService.onEvent(ErrorJoiningLocusEvent)");
        handleJoinError(R.string.call_error_joining, event.getErrorMessage(), event.getErrorCode(), event.Error, event.getUsingResource(), event.getLocusKey(), event.getJoinType());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ConflictErrorJoiningLocusEvent event) {
        Ln.i("CallControlService.onEvent(ConflictErrorJoiningLocusEvent)");
        handleJoinError(R.string.call_error_joining_conflict, event.getErrorMessage(), event.getErrorCode(), event.Error, event.getUsingResource(), event.getLocusKey(), event.getJoinType());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(HighVolumeErrorJoiningLocusEvent event) {
        Ln.i("CallControlService.onEvent(HighVolumeErrorJoiningLocusEvent)");
        handleJoinError(R.string.call_error_joining_high_volume, event.getErrorMessage(), event.getErrorCode(), event.Error, event.getUsingResource(), event.getLocusKey(), event.getJoinType());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(SuccessJoiningLocusEvent event) {
        Ln.i("CallControlService.onEvent(SuccessJoiningLocusEvent)");
        reportJoinLocusMetrics(event.getLocusKey(), event.getUsingResource(), CallJoinMetricValue.SUCCESS, null);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(LocusInviteesExceedMaxSizeEvent event) {
        Ln.i("CallControlService.onEvent(LocusInviteesExceedMaxSizeEvent), code = " + event.getErrorCode());

        // Indication that we exceeded max number of invitees allowed when this participant initiates or adds guests to a meeting. (403)

        String message = "";
        int maxRosterSize = coreFeatures.getMaxRosterSize();
        if (event.isJoin()) { // starting call
            switch (event.getErrorCode()) {
                case LocusExceededMaxNumberParticipantsFreeUser:
                    message = context.getString(R.string.max_participants_warning_free_text, maxRosterSize);
                    break;
                case LocusExceededMaxNumberParticipantsPaidUser:
                case LocusExceededMaxNumberParticipantsTeamMember:
                    message = context.getString(R.string.max_participants_warning_text, maxRosterSize);
                    break;
                case LocusMeetingIsInactive:
                    message = context.getString(R.string.call_error_is_inactive);
                    break;
            }
        } else {  // adding guest
            switch (event.getErrorCode()) {
                case LocusExceededMaxNumberParticipantsFreeUser:
                    message = context.getString(R.string.max_participants_add_guest_warning_free_text, maxRosterSize);
                    break;
                case LocusExceededMaxNumberParticipantsPaidUser:
                case LocusExceededMaxNumberParticipantsTeamMember:
                    message = context.getString(R.string.max_participants_add_guest_warning_text, maxRosterSize);
                    break;
            }
        }

        if (!message.isEmpty()) {
            toaster.showLong(context, message);
        }

        if (event.isJoin()) {
            Call call = getCall(locusKey);
            if (call == null) {
                call = getActiveCall();
            }
            endMediaSession(call, false);
            bus.post(new CallControlCallJoinErrorEvent());
            reportJoinLocusMetrics(locusKey, event.getUsingResource(), event.Error + event.getErrorCode(), event.getErrorMessage());
        }
    }

    public void onEventMainThread(LocusUserIsNotAuthorized event) {
        // Indicate that we received a forbidden (403) response from locus.
        ErrorDetail.CustomErrorCode errorCode =  event.getErrorCode();
        Ln.i("CallControlService.onEventMainThread(LocusForbiddenEvent), code = " + errorCode);
        String errorMessage = "";
        if (event.isJoin()) {
            Call call = getCall(locusKey);
            if (call == null) {
                call = getActiveCall();
            }
            endMediaSession(call, false);
            bus.post(new CallControlCallJoinErrorEvent());
            reportJoinLocusMetrics(locusKey, event.getUsingResource(), event.Error + event.getErrorCode(), event.getErrorMessage());
        }

        if (errorCode == LocusUserIsNotAuthorized) {
            bus.post(new CallControlUserIsNotAuthorized(event.getErrorCode()));
        } else {
            // should never get to here.
            errorMessage = context.getString(R.string.call_error_joining);
            Ln.w("Unable to handle error code = " + errorCode);
        }

        if (!errorMessage.isEmpty()) {
            toaster.showLong(context, errorMessage);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(LocusMeetingLockedEvent event) {
        Ln.i("CallControlService.onEventMainThread(LocusMeetingLockedEvent), code = " + event.getErrorCode());

        // Indication that we received a locked (423) response from Locus.

        String message = "";
        int maxBridgeSize = coreFeatures.getMaxBridgeSize();
        if (event.getErrorCode() != null) {
            switch (event.getErrorCode()) {
                case LocusExceededMaxRosterSizeFreePaidUser:
                case LocusExceededMaxRosterSizeTeamMember:
                    message = context.getString(R.string.max_roster_size_warning_text, maxBridgeSize);
                    break;
                case LocusMeetingIsLocked:
                    message = context.getString(R.string.meeting_locked_warning_text);
                    break;
                case LocusLockedWhileTerminatingPreviousMeeting:
                    message = context.getString(R.string.meeting_locked_while_terminating_previous_meeting);
                    break;
                case LocusMeetingNotStarted:
                    bus.post(new CallControlMeetingNotStartedEvent(event.getErrorCode()));
                    break;
                case LocusRequiresModeratorPINOrGuest:
                    bus.post(new CallControlLocusRequiresModeratorPINOrGuest());
                    break;
                case LocusRequiresModeratorPINorGuestPIN:
                    bus.post(new CallControlLocusRequiresModeratorPINorGuestPIN());
                    break;
            }
        } else {
            message = context.getString(R.string.call_error_joining);
        }

        if (!message.isEmpty()) {
            toaster.showLong(context, message);
        }

        if (event.isJoin()) {
            Call call = getCall(locusKey);
            if (call == null) {
                call = getActiveCall();
            }
            endMediaSession(call, false);

            if (event.getErrorCode() != null &&
                    event.getErrorCode() != ErrorDetail.CustomErrorCode.LocusRequiresModeratorPINOrGuest &&
                    event.getErrorCode() != ErrorDetail.CustomErrorCode.LocusRequiresModeratorPINorGuestPIN) {
                bus.post(new CallControlCallJoinErrorEvent());
                reportJoinLocusMetrics(locusKey, event.getUsingResource(), event.Error + event.getErrorCode(), event.getErrorMessage());
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(JoinedLobbyEvent event) {
        Ln.i("CallControlService.onEvent(JoinedLobbyEvent) Joining Meeting Lobby with LocusKey=%s", event.getLocusKey());
        LocusKey locusKey = event.getLocusKey();
        if (locusKey == null) {
            toaster.showLong(context, context.getString(R.string.call_error_joining));
            return;
        }

        MediaSession activeMediaSession = mediaEngine.getActiveMediaSession();
        if (activeMediaSession != null) {
            activeMediaSession.endSession();
        }

        Call call = getCall(locusKey);
        if (call != null) {
            callAnalyzerReporter.reportMeetingLobbyEntered(call);
        }

        startLobbyKeepAlive(locusKey);
        callUi.showMeetingLobbyUi(event.getLocusKey());
        bus.post(new CallControlJoinedLobbyEvent(event.getLocusKey()));
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(JoinedMeetingFromLobbyEvent event) {
        Ln.i("CallControlService.onEvent(JoinedMeetingFromLobbyEvent): Joining Meeting with LocusKey=%s", event.getLocusKey());
        if (locusService.isJoiningLocus()) {
            Ln.i("CallControlService.onEvent(JoinedMeetingFromLobbyEvent): Exiting. Already joining Locus " + event.getLocusKey());
            return;
        }

        LocusKey activeLocusKey = locusDataCache.getActiveLocus();
        if (activeLocusKey != null && !activeLocusKey.equals(event.getLocusKey())) {
            Ln.w("CallControlService.onEvent(JoinedMeetingFromLobbyEvent):" +
                    "LocusKey %s in the event does not match the currently active LocusKey of %s",
                    event.getLocusKey().toString(),
                    event.getLocusKey().toString());
            return;
        }

        bus.post(new CallControlJoinedMeetingFromLobbyEvent(event.getLocusKey()));

        Call call = getCall(activeLocusKey);

        // Fairly certain this can't be null due to above key check, but for completeness
        if (call == null) {
            Ln.w("CallControlService.onEvent(JoinedMeetingFromLobbyEvent):" +
                    "Call for LocusKey %s is null", activeLocusKey);
            return;
        }

        callAnalyzerReporter.reportMeetingLobbyExited(call);
        boolean isPaired = isInPairedCall(call);

        CallContext updatedCallContext = new CallContext.Builder(event.getLocusKey())
                .setIsAnsweringCall(false)
                .setIsOneOnOne(false)
                .setShowFullScreen(true)
                .setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioVideoShare)
                .setUseRoomPreference((isPaired ? CallContext.UseRoomPreference.UseRoom : CallContext.UseRoomPreference.DontUseRoom))
                .setCallInitiationOrigin(call.getCallInitiationOrigin())
                .setIsMeeting(true).build();

        checkJoinRoomCall(call, updatedCallContext);
    }

    private void handleJoinError(int messageId, String errorMessage, int errorCode, String error, String usingResource, LocusKey locusKey, @JoinType int joinType) {
        toaster.showLong(context, messageId);

        Call call = getCall(locusKey);
        if (call == null) {
            call = getActiveCall();
        }

        // cater for case where we got here as a result of error getting locus in response to GCM incoming call notification....this is still
        // considered join failure but we don't have call object in this case
        if (call != null && joinType == JOIN) {
            endMediaSession(call, false);
            call.setActive(false);
        }

        bus.post(new CallControlCallJoinErrorEvent(joinType));
        reportJoinLocusMetrics(locusKey, usingResource, error + errorCode, errorMessage);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventBackgroundThread(LocusDataCacheChangedEvent event) {
        ln.i("CallControlService.onEvent(LocusDataCacheChangedEvent): %s was %s", event.getLocusKey().toString(), event.getLocusChange().toString());
        updateLocusKeyAndActiveCall(event.getLocusKey());
        if (!locusDataCache.isCallActive(event.getLocusKey())) {
            isCaller = false;
        }
    }

    @SuppressWarnings("UnusedDeclaration") // called by the event bus.
    public void onEventBackgroundThread(LocusDataCacheReplacesEvent event) {
        ln.i("CallControlService.onEvent(LocusDataCacheReplacesEvent: %s with %s)", event.getReplacedLocusKey(), event.getLocusKey());

        dumpJoinedCalls();

        Call call = getCall(event.getReplacedLocusKey());
        if (call != null) {
            ln.i("CallControlService.onEvent(LocusDataCacheReplacesEvent, updating locus key/data in existing call = " + call.getCallId());
            call.setLocusKey(event.getLocusKey());
            call.setLocusData(getLocusData(event.getLocusKey()));
        }
        updateLocusKeyAndActiveCall(event.getLocusKey());

//        if (locusKey.equals(event.getReplacedLocusKey())) {
//            updateLocusKeyAndActiveCall(event.getLocusKey());
//        } else
//            ln.w(false, "CallControlService.onEvent(LocusDataCacheReplacesEvent) LocusKey does not match current key: %s", locusKey);
    }

    private void updateLocusKeyAndActiveCall(LocusKey newLocusKey) {
        // Update local DB flag of Locus joined state
        LocusData locusData = locusDataCache.getLocusData(newLocusKey);
        Ln.d("updateLocusKeyAndActiveCall, locusKey = " + newLocusKey + ", locusData = " + locusData);
        if (locusData != null) {
            if (locusKey == null) {
                locusKey = newLocusKey;
            } else if (!locusKey.equals(newLocusKey) && locusKeyIsReplacement(locusData)) {
                locusKey = newLocusKey;
            }

            Call call = getCall(newLocusKey);
            Ln.d("updateLocusKeyAndActiveCall, call for this locus = " + call);
            if (call == null) {
                LocusParticipantDevice locusParticipantDevice = locusData.getLocus().getMyDevice(deviceRegistration.getUrl());
                Ln.d("updateLocusKeyAndActiveCall, my device = " + locusParticipantDevice);

                if (locusParticipantDevice != null) {
                    Ln.d("updateLocusKeyAndActiveCall, correlation id = " + locusParticipantDevice.getCorrelationId());
                }

                if (locusParticipantDevice != null && locusParticipantDevice.getCorrelationId() != null) {
                    call = getCall(locusParticipantDevice.getCorrelationId());

                    Ln.d("updateLocusKeyAndActiveCall, call for this correlation id = " + call);
                    if (call != null) {
                        call.setLocusKey(newLocusKey);
                    }
                }
            }
            if (call != null) {
                call.setLocusData(locusData);
            }

            setActiveCall(newLocusKey, locusData.getLocus().getFullState().isActive());
        }
        // Might need this after setActiveCall to avoid room less call scenario
        ln.i("CallControlService.onEvent(LocusDataCacheChangedEvent), post CallControlLocusChangedEvent");
        bus.post(new CallControlLocusChangedEvent(newLocusKey));
    }

    private boolean locusKeyIsReplacement(LocusData call) {
        if (call.getLocus().getReplaces() != null && !call.getLocus().getReplaces().isEmpty()) {
            for (LocusReplaces replace : call.getLocus().getReplaces()) {
                setActiveCall(replace.getLocusKey(), false);
                bus.post(new CallNotificationRemoveEvent(replace.getLocusKey()));
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(MediaActiveSpeakerChangedEvent event) {

        Call call = getCall(event.getCallId());
        if (call != null && call.getLocusData() != null) {
            // check if new CSI (capture source identifier) represents a video source and, if so,
            // check if it matches CSI for one of the participants in roster
            Locus locus = call.getLocusData().getLocus();
            if (event.videoChanged()) {
                boolean foundActiveSpeaker = false;
                for (LocusParticipant locusParticipant : locus.getParticipants()) {
                    List<Long> participantCSIs = locusParticipant.getStatus().getCsis();
                    for (int i = 0; i < event.getNewCSIs().length; i++) {
                        long activeCSI = event.getNewCSIs()[i];
                        if (participantCSIs.contains(activeCSI)) {
                            Ln.d("CallControlService.onEvent(MediaActiveSpeakerChangedEvent), active speaker = " + locusParticipant.getPerson().getDisplayName());
                            call.getLocusData().setActiveSpeakerId(locusParticipant.getId());
                            bus.post(new CallControlActiveSpeakerChangedEvent(call.getLocusData(), locusParticipant, event.getVideoId()));
                            foundActiveSpeaker = true;
                            break;
                        }
                    }
                    if (foundActiveSpeaker) break;
                }
                if (!foundActiveSpeaker) {
                    Ln.w("MediaActiveSpeakerChanged did not match any participants");
                }
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(MediaActiveSpeakerVideoMuted event) {
        Ln.i("CallControlService.onEvent(MediaActiveSpeakerVideoMuted): mutedCsi" + event.getCsi());

        Call call = getCall(event.getCallId());
        if (call != null && call.getLocusData() != null) {
            Long mutedCsi = event.getCsi();

            LocusParticipant participant = findParticipantFromCsi(call.getLocusData().getLocus().getParticipants(), mutedCsi);
            if (participant == null) { // this will potentially be the case when getting unmuted notification from linus
                participant = call.getLocusData().getActiveSpeaker();
            } else {
                // set active speaker id, if the active speaker video is muted, MediaActiveSpeakerChangedEvent will not be received.
                if (call.getLocusData().getActiveSpeaker() == null)
                    call.getLocusData().setActiveSpeakerId(participant.getId());
            }

            if (participant != null) {
                bus.post(new CallControlParticipantVideoMutedEvent(call.getLocusKey(), participant, event.isMuted()));
            }
        }
    }

    public void onEvent(MediaStartedEvent event) {
        if (coreFeatures.isCallSpinnerEnabled() && locusDataCache != null && locusDataCache.isInCall()) {
            setSpinnerHideState(true);
        }
    }

    public boolean getSpinnerHideState() {
        return hideSpinner;
    }

    public void setSpinnerHideState(boolean shouldHide) {
        this.hideSpinner = shouldHide;
    }

    private LocusParticipant findParticipantFromCsi(List<LocusParticipant> participants, Long mutedCsi) {
        for (LocusParticipant participant : participants) {
            if (participant.getState() == LocusParticipant.State.JOINED) {
                List<Long> csis = participant.getStatus().getCsis();
                if (csis.contains(mutedCsi)) {
                    Ln.d("foundParticipantFrom CSI person: %s csi: %s csis: %s", participant.getPerson().getDisplayName(), mutedCsi, Arrays.toString(csis.toArray()));
                    return participant;
                }
            }
        }
        Ln.w("could not find participantFrom CSI csi: %s", mutedCsi);
        return null;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(MediaSession.MediaDecodeSizeChangedEvent event) {
        bus.post(new CallControlMediaDecodeSizeChangedEvent(event.vid, event.size));
    }

    public LocusData getLocusData(LocusKey locusKey) {
        return locusService.getLocusData(locusKey);
    }

    public Locus getLocus(LocusKey locusKey) {
        LocusData locusData = getLocusData(locusKey);
        return locusData == null ? null : locusData.getLocus();
    }

    public String getCallOrigin() {
        Call call = getCall(locusKey);
        if (call != null) {
            CallInitiationOrigin origin = call.getCallInitiationOrigin();
            return origin != null ? origin.getValue() : null;
        }

        return null;
    }

    public void setActiveCall(LocusKey locuskey, boolean value) {
        Ln.i("CallControlService.setInActiveCall, locusKey = " + locuskey.toString() + ", value = " + value);
        try {
            Batch batch = batchProvider.get();
            batch.add(updateConversationInActiveCall(locuskey, value));
            batch.apply();
        } catch (Exception ex) {
            Ln.e(ex, "Failed to set 'in active call' field to '%b', for LocusKey: %s", value, locuskey.toString());
        }
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LocusUrlUpdatedEvent event) {
        Ln.d("CallControlService.onEvent(LocusUrlUpdatedEvent): conv url = " + event.getConversationUrl() + ", new locus key = " + event.getNewLocusKey());

        try {
            Batch batch = batchProvider.get();
            batch.add(updateConversationLocusUrl(event.getConversationUrl(), event.getNewLocusKey()));
            batch.apply();

            locusKey = event.getNewLocusKey();
            Call call = getCall(event.getOldLocusKey());
            if (call != null) {
                call.setLocusKey(event.getNewLocusKey());
            }

        } catch (Exception ex) {
            Ln.e(ex, "Failed to updated locus url for conversation");
        }
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(CallControlPhoneStateChangedEvent event) {
        int phoneState = event.getState();
        Ln.d("CallControlService.onEvent(CallControlPhoneStateChangedEvent): state = " + phoneState);

        if (locusDataCache.isInCall()) {
            switch (phoneState) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // For future use
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    MediaSession mediaSession = getMediaSession(locusKey);
                    if (mediaSession != null) {
                        Lns.ux().i("Phone (app) answered--muting outbound andio and video, and inbound audio");
                        wasVideoMuted = mediaSession.getVideoMuteSource();
                        wasAudioMuted = isAudioMuted(locusKey);
                        muteVideo(locusKey, MediaRequestSource.USER);
                        muteAudio(locusKey);
                        muteRemoteAudio(locusKey, true);
                    } else {
                        Lns.ux().i("Phone (app) answered, but there was no local media to mute");
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (wasVideoMuted != null) {
                        Lns.ux().i("Phone (app) hung up--unmuting inbound audio and restoring previous outbound state");
                        if (wasVideoMuted == MediaRequestSource.NONE)
                            unMuteVideo(locusKey, MediaRequestSource.USER);
                        else
                            muteVideo(locusKey, wasVideoMuted);

                        if (wasAudioMuted) {
                            muteAudio(locusKey);
                        } else {
                            unmuteAudio(locusKey);
                        }
                        muteRemoteAudio(locusKey, false);
                        wasVideoMuted = null;
                    }
                    break;
            }
        } else if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
            LocusKey incomingCall = locusDataCache.getIncomingCall();
            if (incomingCall != null) {
                Lns.ux().i("Phone (app) and Spark are both ringing. Dismiss to stop double ringers.");
                callNotification.dismiss(incomingCall);
            }
        }
    }

    public void onEvent(LocusPmrChangedEvent event) {
        bus.post(new CallControlLocusPmrChangedEvent());
    }

    public void onEventMainThread(InvalidLocusEvent event) {
        Call call = getCall(locusKey);
        if (call == null) {
            call = getActiveCall();
        }

        if (event.getJoinType() == JOIN) {
            endMediaSession(call, false);
        }

        bus.post(new CallControlCallJoinErrorEvent());
        bus.post(new CallControlInvalidLocusEvent(event.getErrorCode(), event.getInvitee()));
        reportJoinLocusMetrics(locusKey, event.getUsingResource(), event.Error + event.getErrorCode(), event.getErrorMessage());
    }

    public void onEventMainThread(CallControlModeratorMutedParticipantEvent event) {
        Ln.d("CallControlService.onEvent(CallControlModeratorMutedParticipantEvent), muted = %s remoteParticipant = %s",
                event.isMuted(), event.getParticipant().getPerson().getEmail());
        muteAudioFromRemote(locusKey, event.isMuted());
    }

    @Nullable
    private MediaSession getMediaSession(LocusKey locusKey) {
        Call call = getCall(locusKey);
        if (call != null) {
            return call.getMediaSession();
        }
        return null;
    }

    public boolean isMediaStarted(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            return mediaSession.isMediaStarted();
        }
        return false;
    }

    @Nullable
    public Rect getFullsceenVideoSize(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            return mediaSession.getFullsceenVideoSize();
        }
        return null;
    }

    public void setDisplayRotation(LocusKey locusKey, int screenRotation) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null && autoRotationEnabled(context)) {
            mediaSession.setDisplayRotation(screenRotation);
        }
    }

    public void setPreviewWindow(LocusKey locusKey, View view) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setPreviewWindow(view);
        }
    }

    public void clearPreviewWindow(LocusKey locusKey) {
        setPreviewWindow(locusKey, null);
    }

    public void setActiveSpeakerWindow(LocusKey locusKey, View view) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setActiveSpeakerWindow(view);
        }
    }

    public void removeActiveSpeakerWindow(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.removeActiveSpeakerWindow();
        }
    }

    public boolean isScreenSharing(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        return mediaSession != null && mediaSession.isScreenSharing();
    }

    public void startSelfView(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.startSelfView();
        }
    }

    public void setRemoteWindow(LocusKey locusKey, View view) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setRemoteWindow(view);
        }
    }

    public void setRemoteWindow(LocusKey locusKey, long csi, View view) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setRemoteWindow(csi, view);
        }
    }

    public void removeRemoteWindow(LocusKey locusKey, View view) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.removeRemoteWindow(view);
        }
    }

    public void setShareWindow(LocusKey locusKey, View view) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setShareWindow(view);
        }
    }

    public void removeShareWindow(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.removeShareWindow();
        }
    }

    public void removeRemoteVideoWindows(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.removeRemoteVideoWindows();
        }
    }

    public void setMaxStreamCount(LocusKey locusKey, int maxStreamCount) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setMaxStreamCount(maxStreamCount);
        }
    }

    @Nullable
    public Long checkCSIs(LocusKey locusKey, List<Long> csiList) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            return mediaSession.checkCSIs(csiList);
        }
        return null;
    }

    public void switchCamera(LocusKey locusKey) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.switchCamera();
        }
    }

    public void setAudioSampling(LocusKey locusKey, int duration) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setAudioSampling(duration);
        }
    }

    public void setAudioVolume(int volume) {
        MediaSession mediaSession = getMediaSession(locusKey);
        if (mediaSession != null) {
            mediaSession.setAudioVolume(volume);
        }
    }

    private void startLobbyKeepAlive(final LocusKey locusKey) {

        if (locusKey == null) {
            ln.d("CallControlService.startLobbyKeepAlive, did not start keep-alive for LocusKey %s as it is NULL");
            return;
        }

        if (!locusDataCache.exists(locusKey)) {
            ln.d("CallControlService.startLobbyKeepAlive, did not start keep-alive for LocusKey %s as it is not in the cache", locusKey);
            return;
        }

        Locus locus = getLocusData(locusKey).getLocus();

        if (locus != null && locus.isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
            synchronized (lobbyTimerLock) {
                if (lobbyKeepAliveTimer != null) {
                    lobbyKeepAliveTimer.cancel();
                    lobbyKeepAliveTimer.purge();
                    lobbyKeepAliveTimer = null;
                }
            }

            LocusParticipantDevice currentDevice = locusService.getCurrentDevice(locusDataCache.getLocusData(locusKey));
            int keepAliveInSeconds = currentDevice != null ? (currentDevice.getKeepAliveSecs() / 2) : 0;
            int keepAliveInMillis = keepAliveInSeconds * 1000;
            if (keepAliveInSeconds > 0) {
                ln.d("CallControlService.startLobbyKeepAlive, starting Lobby keep-alive for PMR %s with value of %s seconds.", locusKey.toString(), keepAliveInSeconds);
                synchronized (lobbyTimerLock) {
                    lobbyKeepAliveTimer = new Timer();
                    lobbyKeepAliveTimer.schedule(new LobbyKeepAliveTimerTask(locusKey), keepAliveInMillis, keepAliveInMillis);
                }
            }
        } else {
            ln.d("CallControlService.startLobbyKeepAlive, did not start keep-alive for LocusKey %s as it is either NULL, not in the cache, or is not a PMR.", locusKey);
        }
    }

    private class LobbyKeepAliveTimerTask extends TimerTask {
        private final LocusKey locusKey;

        public LobbyKeepAliveTimerTask(@NonNull LocusKey locusKey) {
            this.locusKey = locusKey;
        }

        @Override
        public void run() {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null && locusData.getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
                ln.d("CallControlService.LobbyKeepAliveTimerTask, sending keep-alive for PMR %s.", locusKey);
                locusService.keepAlive(locusKey);
            } else {
                ln.d("CallControlService.LobbyKeepAliveTimerTask, stopping Lobby keep-alive for PMR %s.", locusKey);
                this.cancel();
            }
        }
    }


    public void toggleAudioCall(LocusKey locusKey) {
        ln.d("CallControlService.toggleAudioCall, locus = %s", locusKey);
        Call call = getCall(locusKey);
        if (call != null) {
            call.setAudioCall(!call.isAudioCall());

            if (call.isAudioCall()) {
                updateMediaSession(call, MediaEngine.MediaDirection.SendReceiveAudioOnly);
            } else {
                updateMediaSession(call, MediaEngine.MediaDirection.SendReceiveAudioVideoShare);
            }

            callMetricsReporter.reportCallAudioToggleMetrics(call);

            bus.post(new CallControlAudioOnlyStateChangedEvent(locusKey));
        }
    }

    // TODO if we end up getting some sort of meeting type from Locus in the future, remove use of this
    public boolean isCaller() {
        return isCaller;
    }



    public synchronized void onEvent(RoapMessageEvent event) {
        Ln.d("CallControlService.onEvent(RoapMessageEvent), type = " + event.getMessage().getMessageType());

        // look up call that this ROAP event is for (using correlation id).  Delegate to ROAP session
        // state machine to process event.
        Call call = getCall(event.getCorrelationId());
        if (call != null) {
            RoapSession roapSession = call.getRoapSession();
            roapSession.processRoapMessage(event.getMessage());
        }
    }


    @Override
    public void sendRoapMessage(RoapBaseMessage roapMessage) {
        Ln.d("CallControlService.send(RoapBaseMessage), type = " + roapMessage.getMessageType());

        // send locus modifyMedia request with this ROAP message (this method will be called by RoapSession
        // state machine when it needs to send a ROAP message.
        Call call = getActiveCall();
        if (call != null) {
            List<MediaConnection> mediaConnectionList = new ArrayList<>();
            MediaInfo mediaInfo = new MediaInfo(roapMessage);

            MediaSession mediaSession = call.getMediaSession();

            // set audio/videoMuted to whatever they were previously set to so that locus does not interpret this
            // as a toggle of those values (in which case it doesn't send updated SDP to Calliope)
            mediaInfo.setVideoMuted(mediaSession.isVideoMuted());
            mediaInfo.setAudioMuted(mediaSession.isAudioMuted());

            MediaConnection mediaConnection = new MediaConnection();
            if (call.getLocusData() != null) {
                // set media ID
                MediaConnection currentMediaConnection = getMediaConnection(call.getLocusData().getLocus());
                if (currentMediaConnection != null) {
                    mediaConnection.setMediaId(currentMediaConnection.getMediaId());
                }
            }

            mediaConnection.setLocalSdp(gson.toJson(mediaInfo));
            mediaConnectionList.add(mediaConnection);
            locusService.modifyMedia(call.getLocusKey(), mediaConnectionList, mediaSession.isAudioMuted());
        }
    }

    @Override
    public void receivedRoapAnswer(List<String> sdpList) {
        Ln.d("CallControlService.receivedRoapAnswer");

        // received ROAP ANSWER....send OK response and update WME
        Call call = getActiveCall();
        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null && sdpList != null && sdpList.size() > 0) {

                RoapSession roapSession = call.getRoapSession();
                roapSession.sendOK();

                String sdp = sdpList.get(0);
                if (!isMediaStarted(locusKey)) {
                    Ln.d("CallControlService.receivedRoapAnswer, calling startMedia/answerReceived");
                    mediaSession.startMedia();
                    mediaSession.answerReceived(sdp, null);
                    logStartMediaStats(call);
                } else {
                    Ln.d("CallControlService.receivedRoapAnswer, calling updateSDP");
                    mediaSession.updateSDP(sdp);
                }
            }
        }
    }


    @Override
    public void receivedRoapOffer(List<String> sdpList) {
        Ln.d("CallControlService.receivedRoapOffer");

        // received ROAP OFFER request....reply with ANSWER
        Call call = getActiveCall();
        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();

            if (mediaSession != null && sdpList != null & sdpList.size() > 0) {
                String offerSdp = sdpList.get(0);
                mediaSession.offerReceived(offerSdp);

                mediaSession.createAnswer(sdp -> {
                    RoapSession roapSession = call.getRoapSession();
                    roapSession.sendRoapMessage(sdp, 0L);

                    // I'm not sure the call-analyzer folks have considered offer/answer.
                    // Let's just call this local SDP for now
                    callAnalyzerReporter.reportLocalSdpGenerated(call);
                });
            }
        }
    }



    // These methods are implemented by each of the clients

    // Default base implementation
    public boolean shouldReportCallLogs(Call call) {
        return call.isCallConnected();
    }

    // Default base implementation
    public boolean isJoiningCall() {
        return locusService.isJoiningLocus();
    }



    public Call getCall(String callId) {
        synchronized (joinedCalls) {
            return joinedCalls.get(callId);
        }
    }

    public Call getCall(LocusKey locusKey) {
        synchronized (joinedCalls) {
            for (Call call : joinedCalls.values()) {
                if (call.isActive() && call.getLocusKey() != null && call.getLocusKey().equals(locusKey)) {
                    return call;
                }
            }
        }
        return null;
    }

    public void dumpJoinedCalls() {
        Ln.v("----------------------------------------------------------------------------------------------------------------------");
        Ln.v("JoinedCalls:");
        synchronized (joinedCalls) {
            for (Call call : joinedCalls.values()) {
                Ln.v(call.getCallId() + ", mediaSession = " + call.getMediaSession() + ", locus = " + call.getLocusKey() + ", active = " + call.isActive());
            }
        }
        Ln.v("----------------------------------------------------------------------------------------------------------------------");
    }



    public Call getActiveCall() {
        synchronized (joinedCalls) {
            for (Call call : joinedCalls.values()) {
                if (call.isActive()) {
                    return call;
                }
            }
        }
        return null;
    }


    public void requestAudioFocus(MediaEngine.MediaDirection mediaDirection) { }

    public void abandonAudioFocus() { }

    public void handleRoomCallEnd() { }

    public void sendLeaveLocusEvent(Call call) { }

    public void reportCallCancelledMetrics(Call call) { }

    public void updateRoomCopyCall(boolean isObserving, RoomUpdatedEvent.RoomState roomState, boolean localMedia) { }

    public void handleMediaDeviceRegistration() { }

    public void setRoomJoined() { }

    public void setRoomCallNotifying() { }

    public void setRoomCallConnected(String conversationTitle) { }

    public void sendCallNotificationEvent(LocusKey localLocusKey, LocusData call) { }

    private void reportJoinLocusMetrics(LocusKey locusKey, String usingResource, String result, String detailErrorMessage) {
        callMetricsReporter.reportJoinLocusMetrics(locusKey, usingResource, result, detailErrorMessage);
    }

    // ---------------------------------------------------------------------------------------------
    // MediaShare (Share, Unshare, FloorGranted, FloorReleasedEvent, FloorLostEvent)
    // ---------------------------------------------------------------------------------------------

    public void shareScreen(LocusKey locusKey) {
        Call call = getCall(locusKey);
        if (call != null) {
            locusService.shareScreen(locusKey);
            callAnalyzerReporter.reportShareInitiated(call, MediaType.CONTENT_SHARE);
        }
    }

    public void unshareScreen(LocusKey locusKey) {
        Call call = getCall(locusKey);
        if (call != null) {
            locusService.unshareScreen(locusKey);
            callAnalyzerReporter.reportShareStopped(call, MediaType.CONTENT_SHARE);
        }
    }

    public void shareWhiteboard(String whiteboardUrl) {
        locusService.shareWhiteboard(locusKey, whiteboardUrl);
        callAnalyzerReporter.reportShareInitiated(getCall(locusKey), MediaType.WHITEBOARD);
    }

    public void unshareWhiteboard() {
        if (locusService.isSharingWhiteboard()) {
            locusService.unshareWhiteboard(locusKey);
            callAnalyzerReporter.reportShareStopped(getCall(locusKey), MediaType.WHITEBOARD);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(FloorGrantedEvent event) {
        if (!isValidFloorEvent(event.getLocusKey(), event.getClass().getSimpleName())) return;

        Call call = getCall(event.getLocusKey());
        MediaShare mediaShare = call.getLocusData().getLocus().getGrantedFloor();
        startMediaShare(call, mediaShare);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(FloorLostEvent event) {
        if (!isValidFloorEvent(event.getLocusKey(), event.getClass().getSimpleName())) return;
        // stop previous sharing
        stopMediaShare(getCall(event.getLocusKey()), event.getLocalMediaShare(), event.getRemoteMediaShare());
        // post FloorGrantedEvent to init new sharing
        Ln.i("CallControlService.onEvent(FloorLostEvent): post FloorGrantedEvent");
        bus.post(new FloorGrantedEvent(event.getLocusKey()));
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(FloorReleasedEvent event) {
        if (!isValidFloorEvent(event.getLocusKey(), event.getClass().getSimpleName())) return;
        Call call = getCall(event.getLocusKey());
        MediaShare mediaShare = null;
        if (MediaShare.SHARE_CONTENT_TYPE.equals(event.getShareType())) {
            mediaShare = call.getLocusData().getLocus().getShareContentMedia();
        } else if (MediaShare.SHARE_WHITEBOARD_TYPE.equals(event.getShareType())) {
            mediaShare = call.getLocusData().getLocus().getWhiteboardMedia();
        }
        stopMediaShare(call, mediaShare, null);
    }

    private boolean isValidFloorEvent(LocusKey locusKey, String floorType) {
        ln.i("CallControlService.onEvent(%s): %s", floorType, locusKey);

        if (!locusDataCache.isInCall()) {
            ln.i("CallControlService.onEvent(%s), not joined from this device, so ignore", floorType);
            return false;
        }

        Call call = getCall(locusKey);
        if (call == null) {
            ln.w("CallControlService.onEvent(%s), could not find call with locusKey: %s", floorType, locusKey);
            return false;
        }

        if (call.getLocusData() == null) {
            ln.e("CallControlService.onEvent(%s), LocusData in Call is null with locusKey: %s", floorType, locusKey);
            return false;
        }

        return true;
    }

    private void stopMediaShare(Call call, MediaShare local, MediaShare remote) {
        if (local == null) {
            ln.e("CallControlService.stopMediaShare, mediaShare is null");
            return;
        }
        if (local.isContent()) {
            stopContentShare(call, remote);
        } else if (local.isWhiteboard()) {
            stopWhiteboardShare(call, local.getUrl());
        } else {
            ln.e("CallControlService.stopMediaShare, mediaShare's type in Call is unknown with locusKey: %s", call.getLocusKey());
        }
    }

    private void stopContentShare(Call call, MediaShare remote) {
        // Set MediaSession State
        MediaSession mediaSession = call.getMediaSession();
        if (mediaSession != null) {
            mediaSession.updateShareId("");
            mediaSession.leaveShare(call.getLocusData().getFloorGrantedId());

            boolean sharingContentFromMine = remote != null
                    && remote.isContent()
                    && remote.getFloor().getBeneficiary().getDeviceUrl().equals(deviceRegistration.getUrl());

            // When some else is sharing his/her screen and I start sharing screen,
            // Don't stop screen sharing here.
            if (!sharingContentFromMine) {
                if (mediaSession.isScreenSharing()) {
                    mediaSession.stopScreenShare(null);
                }

                if (isInPairedCall(call)) {
                    mediaSession.endSession();
                    locusService.deleteMedia(call.getLocusKey(), getMediaConnection(call.getLocusData().getLocus()));
                    call.setMediaSession(null);
                }
            }
            bus.post(new CallControlFloorReleasedEvent(call.getLocusKey()));
        }
    }

    private void stopWhiteboardShare(Call call, Uri uri) {
        if (uri != null) {
            // set false to help Whiteboard UI elements set their states
            locusService.setSharingWhiteboard(false);
            bus.post(new CallControlEndWhiteboardShare(call.getLocusKey(), uri));
            ln.e("CallControlService.stopWhiteboardShare, mediaShare(Whiteboard)'s url in Call is null with locusKey: %s", call.getLocusKey());
        }
    }

    private void startMediaShare(Call call, MediaShare remote) {
        if (remote == null) {
            ln.e("CallControlService.startMediaShare, mediaShare is null");
            return;
        }
        if (remote.isContent()) {
            startContentShare(call, remote);
        } else if (remote.isWhiteboard()) {
            startWhiteboardShare(call, remote.getUrl());
        } else {
            ln.e("CallControlService.startMediaShare, mediaShare's type in Call is unknown with locusKey: %s", call.getLocusKey());
        }
    }

    private void startContentShare(Call call, MediaShare remote) {
        MediaSession mediaSession = call.getMediaSession();
        if (isInPairedCall(call) && mediaSession == null) {
            startMediaSession(call, MediaEngine.MediaDirection.SendReceiveShareOnly);
            mediaSession = call.getMediaSession();
        }

        boolean sharingContentFromMine = remote != null
                && remote.isContent()
                && remote.getFloor().getBeneficiary().getDeviceUrl().equals(deviceRegistration.getUrl());

        String grantedId = DateUtils.formatUTCDateString(remote.getFloor().getGranted());

        if (!sharingContentFromMine) {
            mediaSession.joinShare(grantedId);
        } else {
            mediaSession.startScreenShare(grantedId);
        }
        mediaSession.updateShareId(grantedId);
        bus.post(new CallControlFloorGrantedEvent(call.getLocusKey()));
    }

    private void startWhiteboardShare(Call call, Uri uri) {
        if (uri != null) {
            // set true to help Whiteboard UI elements set their states
            locusService.setSharingWhiteboard(true);
            bus.post(new CallControlViewWhiteboardShare(call.getLocusKey(), uri));
        } else {
            ln.e("CallControlService.startWhiteboardShare, mediaShare(Whiteboard)'s url in Call is null with locusKey: %s", call.getLocusKey());
        }
    }
}
