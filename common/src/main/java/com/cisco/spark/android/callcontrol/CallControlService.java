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
import com.cisco.spark.android.callcontrol.events.CallControlDisableVideoEvent;
import com.cisco.spark.android.callcontrol.events.CallControlDisconnectedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlEndLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlEndWhiteboardShare;
import com.cisco.spark.android.callcontrol.events.CallControlFloorGrantedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlFloorReleasedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlHeldEvent;
import com.cisco.spark.android.callcontrol.events.CallControlInvalidLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlJoinedLobbyEvent;
import com.cisco.spark.android.callcontrol.events.CallControlJoinedMeetingEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLeaveLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalAudioMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusCreatedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusPmrChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLostEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsExpelEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingNotStartedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlNumericDialingPreventedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantJoinedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlPhoneStateChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlReconnectEvent;
import com.cisco.spark.android.callcontrol.events.CallControlResumedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlSelfParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlViewDesktopShare;
import com.cisco.spark.android.callcontrol.events.CallControlViewWhiteboardShare;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.PermissionsHelper;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.events.CallNotificationEvent;
import com.cisco.spark.android.events.CallNotificationRemoveEvent;
import com.cisco.spark.android.events.CallNotificationType;
import com.cisco.spark.android.events.CallNotificationUpdateEvent;
import com.cisco.spark.android.events.RequestCallingPermissions;
import com.cisco.spark.android.locus.events.AnsweredInactiveCallEvent;
import com.cisco.spark.android.locus.events.ConflictErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.ErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.FloorGrantedEvent;
import com.cisco.spark.android.locus.events.FloorReleasedEvent;
import com.cisco.spark.android.locus.events.FloorRequestAcceptedEvent;
import com.cisco.spark.android.locus.events.FloorRequestDeniedEvent;
import com.cisco.spark.android.locus.events.HighVolumeErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.IncomingCallEvent;
import com.cisco.spark.android.locus.events.JoinedLobbyEvent;
import com.cisco.spark.android.locus.events.JoinedMeetingEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheChangedEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheReplacesEvent;
import com.cisco.spark.android.locus.events.InvalidLocusEvent;
import com.cisco.spark.android.locus.events.LocusInviteesExceedMaxSizeEvent;
import com.cisco.spark.android.locus.events.LocusLeftEvent;
import com.cisco.spark.android.locus.events.LocusMeetingLockedEvent;
import com.cisco.spark.android.locus.events.LocusPmrChangedEvent;
import com.cisco.spark.android.locus.events.LocusUrlUpdatedEvent;
import com.cisco.spark.android.locus.events.ParticipantChangedEvent;
import com.cisco.spark.android.locus.events.ParticipantDeclinedEvent;
import com.cisco.spark.android.locus.events.ParticipantJoinedEvent;
import com.cisco.spark.android.locus.events.ParticipantLeftEvent;
import com.cisco.spark.android.locus.events.ParticipantSelfChangedEvent;
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
import com.cisco.spark.android.locus.model.LocusReplaces;
import com.cisco.spark.android.locus.model.LocusSdp;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.model.MediaConnection;
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
import com.cisco.spark.android.media.MediaCallbackObserver;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaRequestSource;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.media.MediaSessionUtils;
import com.cisco.spark.android.media.ScreenShareCallback;
import com.cisco.spark.android.media.events.DeviceCameraUnavailable;
import com.cisco.spark.android.media.events.FirstAudioPacketReceivedEvent;
import com.cisco.spark.android.media.events.ICEConnectionFailedEvent;
import com.cisco.spark.android.media.events.MediaActiveSpeakerChangedEvent;
import com.cisco.spark.android.media.events.MediaActiveSpeakerVideoMuted;
import com.cisco.spark.android.media.events.NetworkCongestionEvent;
import com.cisco.spark.android.media.events.NetworkDisableVideoEvent;
import com.cisco.spark.android.media.events.NetworkDisconnectEvent;
import com.cisco.spark.android.media.events.NetworkLostEvent;
import com.cisco.spark.android.media.events.NetworkReconnectEvent;
import com.cisco.spark.android.media.events.StunTraceResultEvent;
import com.cisco.spark.android.media.statistics.MediaStats;
import com.cisco.spark.android.mercury.events.DeclineReason;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.model.Person;
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
import static com.cisco.spark.android.sync.ConversationContentProviderOperation.updateConversationInActiveCall;
import static com.cisco.spark.android.sync.ConversationContentProviderOperation.updateConversationLocusUrl;
import static com.cisco.spark.android.util.UIUtils.autoRotationEnabled;
import static com.cisco.spark.android.whiteboard.util.WhiteboardUtils.getIdFromUrl;

/**
 * The purpose of this class is to provide a general call control abstraction that's responsible for orchestrating
 * the activities of the locus signalling and media engine components.
 */
public class CallControlService implements MediaCallbackObserver {
    public static final String AUDIO_TYPE = "AUDIO";
    public static final String VIDEO_TYPE = "VIDEO";

    protected final LocusService locusService;
    protected final CallMetricsReporter callMetricsReporter;
    private final MediaEngine mediaEngine;
    protected final EventBus bus;
    protected final Context context;
    private final TrackingIdGenerator trackingIdGenerator;
    private final DeviceRegistration deviceRegistration;
    private final LogFilePrint logFilePrint;
    private final Gson gson;
    private final CallNotification callNotification;
    protected final LocusDataCache locusDataCache;
    private final Provider<Batch> batchProvider;
    protected final CallUi callUi;
    private final UploadLogsService uploadLogsService;
    protected final Settings settings;
    private final PermissionsHelper permissionsHelper;
    protected final NaturalLog ln;
    private final LinusReachabilityService linusReachabilityService;
    private final SdkClient sdkClient;
    private final Object syncLock = new Object();

    private CallContext callContext;
    private boolean dtmfReceiveSupported;

    protected LocusKey locusKey;
    protected String joinLocusTrackingID;

    private boolean wasAudioMuted;
    private MediaRequestSource wasVideoMuted;

    private Handler unansweredHandler;
    private boolean requestingFloor;
    private boolean isCaller;
    private boolean audioMutedLocally;

    private final ScreenShareCallback screenShareCallback;

    private boolean videoBlocked;
    private Action0 unblockedAction;

    private Timer lobbyKeepAliveTimer;

    public CallControlService(LocusService locusService, final MediaEngine mediaEngine, CallMetricsReporter callMetricsReporter,
                              EventBus bus, Context context,
                              TrackingIdGenerator trackingIdGenerator, DeviceRegistration deviceRegistration, LogFilePrint logFilePrint,
                              Gson gson, UploadLogsService uploadLogsService, CallNotification callNotification, LocusDataCache locusDataCache,
                              Settings settings, Provider<Batch> batchProvider, Ln.Context lnContext, CallUi callUi,
                              LinusReachabilityService linusReachabilityService, SdkClient sdkClient) {
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
        this.ln = Ln.get(lnContext, "CallControlService");
        this.permissionsHelper = new PermissionsHelper(context);
        this.linusReachabilityService = linusReachabilityService;
        this.sdkClient = sdkClient;
        this.screenShareCallback = new ScreenShareCallback() {
            @Override
            public void onShareStopped() {
                unshareScreen();
            }
        };
        bus.register(this);
    }

    private void joinMeeting(final CallContext callContext) {
        Ln.i("CallControlService.joinMeeting, locusKey = " + callContext.getLocusKey());
        this.callContext = callContext;
        this.locusKey = callContext.getLocusKey();

        callUi.showInCallUi(callContext, false);

        // kick off sdp offer/answer
        setupCallSettings();
        startMediaSession(this, callContext.getMediaDirection());
    }

    public void updateLocusWithPin(LocusKey locusKey, String pin) {
        // User must already be in the Lobby of the meeting from this device.
        LocusData locusData = locusDataCache.getLocusData(locusKey);
        if (this.locusKey.equals(locusKey) && locusData != null && locusData.getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
            locusService.updateLocusWithMeetingPin(locusKey, pin);
        }
    }

    /**
     * Join call.  This is used for joining an existing locus or  making 1:1 call
     * to user/endpoint using locus /call api.
     */
    public void joinCall(final CallContext callContext) {
        Ln.i("CallControlService.joinCall, locusKey = " + callContext.getLocusKey() + ", isOneOnOne = " + callContext.isOneOnOne());


        // if making call to uri then don't allow dialing numeric uris if associated feature is not enabled
        if (!TextUtils.isEmpty(callContext.getInvitee())) {
            if (!deviceRegistration.getFeatures().isNumericDialingEnabled() && Strings.isPhoneNumber(callContext.getInvitee())) {
                callMetricsReporter.reportCallNumericDialPrevented();
                bus.post(new CallControlNumericDialingPreventedEvent());
                return;
            }
        }

        if (!permissionsHelper.hasCameraPermission() || !permissionsHelper.hasMicrophonePermission()) {
            if (callContext.isFromNotification() || callContext.isCrossLaunch()) {
                callUi.requestCallPermissions(callContext);
            } else {
                bus.post(new RequestCallingPermissions(callContext));
            }
            return;
        }

        this.callContext = callContext;
        this.locusKey = callContext.getLocusKey();
        logCall(LogCallIndex.JOINING, getLocusData(locusKey));

        setupCallSettings();

        checkJoinRoomCall(callContext);
    }

    public void joinDesktopShare() {
        CallContext callContext = new CallContext.Builder(locusKey)
                .setShowFullScreen(true)
                .setMediaDirection(MediaEngine.MediaDirection.SendReceiveShareOnly)
                .build();

        joinCall(callContext);

        LocusData call = getLocusData(locusKey);
        if (call == null || call.getLocus() == null) return;

        MediaShare mediaShare = call.getLocus().getGrantedFloor();
        if (mediaShare != null && mediaShare.isContent()) {
            bus.post(new CallControlViewDesktopShare(locusKey));
        }

        MediaSession mediaSession = call.getMediaSession();
        if (mediaSession != null) {
            mediaSession.joinShare(call.getFloorGrantedId());
        } else {
            Floor floor = mediaShare != null ? mediaShare.getFloor() : null;
            LocusParticipant selfParticipant = call.getLocus().getSelf();
            if (floor != null && selfParticipant != null) {
                floor.setBeneficiary(selfParticipant);
            }
        }
    }

    /**
     * Check Join Room Call.  This decides if the call should use a room for media in SquaredCallControlService.
     */
    public void checkJoinRoomCall(CallContext callContext) {
        callUi.showInCallUi(callContext, false);

        startMediaSession(this, callContext.getMediaDirection());
    }

    protected void startMediaSession(MediaCallbackObserver mediaCallbackObserver, MediaEngine.MediaDirection mediaDirection) {
        if (callContext == null) {
            Ln.w("startMediaSession() was called while callContext was null");
            return;
        } else if (mediaDirection == null) {
            mediaDirection = MediaEngine.MediaDirection.SendReceiveAudioVideoShare;
        }

        requestAudioFocus(mediaDirection);

        MediaSession mediaSession = mediaEngine.startMediaSession(mediaCallbackObserver, mediaDirection);
        callContext.setMediaSession(mediaSession);
    }


    protected void updateMediaSession(MediaEngine.MediaDirection mediaDirection) {
        LocusData call = getLocusData(locusKey);
        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                mediaSession.updateSession(this, mediaDirection);
            }
        }
    }

    private void endMediaSession(boolean logMediaStats) {
        Ln.d("CallControlService.endMediaSession, locusKey = %s", locusKey);

        MediaSession mediaSession = null;
        LocusData call = getLocusData(locusKey);
        if (call != null) {
            mediaSession = call.getMediaSession();
        } else if (callContext != null) {
            mediaSession = callContext.getMediaSession();
        }

        if (mediaSession != null) {
            if (logMediaStats) {
                // log media stats for ABC/ABS testing
                logEndMediaStats(mediaSession);
            }
            if (mediaSession.isScreenSharing()) {
                mediaSession.stopScreenShare(call != null ? call.getFloorGrantedId() : "");
            }

            mediaSession.stopMedia();
            mediaSession.endSession();

            // metrics reporting code will need access to media session info later on
            callMetricsReporter.setMediaSession(mediaSession);

            if (call != null) {
                call.setMediaSession(null);
            }

            abandonAudioFocus();

            bus.post(new CallControlMediaSessionStoppedEvent());
        }
    }

    /**
     * This will get called from media engine (in response to createOffer()) as part of SDP offer/answer flow.  The offer SDP provided here
     * gets passed down to locus with the negotiated (answer) SDP in response (this then gets passed down to media engine answerReceived()
     * which triggers appropriate media track logic)
     */
    @Override
    public void onSDPReady(MediaSession mediaSession, String sdp) {
        if (sdp != null) {
            List<MediaConnection> mediaConnectionList = new ArrayList<>();
            String calliopeSupplementaryInformationString = buildCalliopeSupplementaryInformation();

            Map<String, Object> clusterInfo = linusReachabilityService.getLatestLinusReachabilityResults();
            Ln.d("On SDP Ready, clusterInfo is " + clusterInfo);
            LocusSdp locusSdp = new LocusSdp(sdp, MediaEngine.SDP_TYPE, calliopeSupplementaryInformationString, clusterInfo);

            locusSdp.setDtmfReceiveSupported(dtmfReceiveSupported);

            // set audio/videoMuted to whatever they were previously set to so that locus does not interpret this
            // as a toggle of those values (in which case it doesn't send updated SDP to Calliope)
            locusSdp.setVideoMuted(mediaSession.isVideoMuted());
            locusSdp.setAudioMuted(mediaSession.isAudioMuted());

            MediaConnection mediaConnection = new MediaConnection();
            mediaConnection.setType("SDP");
            mediaConnection.setLocalSdp(gson.toJson(locusSdp));
            mediaConnectionList.add(mediaConnection);

            LocusData call = locusDataCache.getLocusData(locusKey);

            // This is very hard to understand and maintain.
            if (call != null && isInPairedCall(call) && !TextUtils.isEmpty(callContext.getUsingResource())) {
                ln.i("CallControlService.onSDPReady, adding (share) media to existing paired call");

                // JOR TODO make sure we're handling creation/deletion of associated MediaSession
                locusService.createMedia(locusKey, mediaConnection);
            } else if (call != null && call.getLocus().isJoinedFromThisDevice(deviceRegistration.getUrl()) && !isCopyingCallFromTp(call)) {
                ln.i("CallControlService.onSDPReady, modifying media for existing call");
                locusService.modifyMedia(locusKey, mediaConnectionList);
            } else {
                ln.d("CallControlService.onSDPReady, %s", call == null ? "joining new call" : "transitioning call from remote media to local media");
                // if bridge call (where locusKey already exists), use joinLocus; otherwise use call api
                if (callContext.getLocusKey() != null) {
                    locusService.joinLocus(locusKey, mediaConnectionList, callContext);
                } else {
                    isCaller = true;
                    locusService.call(callContext.getInvitee(), mediaConnectionList, callContext);
                }
                joinLocusTrackingID = trackingIdGenerator.currentTrackingId();
            }
        }
    }

    /**
     * This will get called from media engine. It is a trimmed down version of onMediaBlocked, just for changes in video of the active speaker.
     * Listeners can register a callback action to invoke when video changes from blocked to unblocked.  The significance of this transition
     * versus others is, full frame live video is available this action is called.
     */
    @Override
    public void onVideoBlocked(boolean blocked) {
        Ln.d("CallControlService.onVideoBlocked(%b), videoBlocked=%b, unblockedAction=%s", blocked, videoBlocked, unblockedAction == null ? "null" : "non-null");
        if (videoBlocked && !blocked && unblockedAction != null)
            unblockedAction.call();
        videoBlocked = blocked;
    }

    public void registerVideoUnblockedAction(Action0 action) {
        Ln.d("CallControlService.registerVideoUnblockedAction");
        unblockedAction = action;
    }

    public void unregisterVideoUnblockedAction() {
        Ln.d("CallControlService.unregisterVideoUnblockedAction");
        unblockedAction = null;
    }

    private boolean isCopyingCallFromTp(LocusData call) {
        return isInPairedCall(call) && !TextUtils.isEmpty(call.getObservingResource()) && callContext.getUsingResource() == null;
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

        LocusData call = getLocusData(locusKey);
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

        LocusData call = getLocusData(locusKey);
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

        LocusData secondCall = getLocusData(secondLocusKey);
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

        LocusData secondCall = getLocusData(secondLocusKey);
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
            LocusInvitee locusInvitee = new LocusInvitee();
            locusInvitee.setInvitee(person.getEmail());
            invitees.add(locusInvitee);

        }
        locusService.addUsersToLocus(locusKey, invitees);
    }



    private void endCallAndLeaveLocusSync() throws Exception {
        requestingFloor = false;
        locusService.setSharingWhiteboard(false);

        LocusData call = getLocusData(locusKey);
        if (call != null) {
            callUi.dismissRingback(locusKey);
            stopUnansweredHandler();

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                call.setWasMediaFlowing(mediaSession.wasMediaFlowing());
            }
            call.setCallStarted(false);
            endMediaSession(true);

            handleRoomCallEnd();

            ln.i("CallControlService.endCall, post CallControlEndLocusEvent");
            bus.post(new CallControlEndLocusEvent(locusKey));

            // If self where set to a left state remotely, this happens when we leave with observing
            // resource, do not attempt to leave again, as we are already left.
            LocusSelfRepresentation self = call.getLocus().getSelf();
            Ln.i("endCall, self state = " + (self != null ? self.getState() : "<not present>"));
            if ((self == null || self.getState() != LocusParticipant.State.LEFT)) {
                locusService.leaveLocusSync(locusKey, call.getObservingResource());
            } else {
                ln.i("Not leaving as self is missing or self state != LEFT");
                call.setMediaSession(null);
            }
        }
    }


    /**
     * End Call
     */
    public synchronized void endCall() {
        ln.i("CallControlService.endCall, locus = " + locusKey);

        requestingFloor = false;
        locusService.setSharingWhiteboard(false);

        LocusData call = getLocusData(locusKey);
        if (call != null) {
            callUi.dismissRingback(locusKey);
            stopUnansweredHandler();

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                call.setWasMediaFlowing(mediaSession.wasMediaFlowing());
            }
            call.setCallStarted(false);
            endMediaSession(true);

            handleRoomCallEnd();

            // If self where set to a left state remotely, this happens when we leave with observing
            // resource, do not attempt to leave again, as we are already left.
            LocusSelfRepresentation self = call.getLocus().getSelf();
            Ln.i("endCall, self state = " + (self != null ? self.getState() : "<not present>"));
            if ((self == null || self.getState() != LocusParticipant.State.LEFT)) {
                String observingResource = call.getObservingResource();
                if (observingResource != null) {
                    locusService.leaveLocus(locusKey, observingResource);
                } else {
                    locusService.leaveLocus(locusKey);
                }
            } else {
                ln.i("Not leaving as self is missing or self state != LEFT");
                call.setMediaSession(null);
            }
            ln.i("CallControlService.endCall, post CallControlEndLocusEvent");
            bus.post(new CallControlEndLocusEvent(locusKey));
        }
    }

    /**
     * Share Screen
     */
    public void shareScreen() {
        LocusData call = getLocusData(locusKey);
        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null && !requestingFloor) {
                requestingFloor = true;
                if (mediaSession.isMediaStarted()) {
                    locusService.shareScreen(locusKey);
                } else {
                    mediaEngine.startMediaSession(this, MediaEngine.MediaDirection.SendReceiveShareOnly);
                }
            }
        }
    }

    public void unshareScreen() {
        LocusData call = getLocusData(locusKey);
        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null && mediaSession.isScreenSharing()) {
                mediaSession.stopScreenShare(call.getFloorGrantedId());
                locusService.unshareScreen(locusKey);
            }
        }
    }

    public void shareWhiteboard(String whiteboardUrl) {
        if (!requestingFloor) {
            requestingFloor = true;
            locusService.shareWhiteboard(locusKey, whiteboardUrl);
        }
    }

    public void unshareWhiteboard() {
        if (locusService.isSharingWhiteboard()) {
            locusService.unshareWhiteboard(locusKey);
            requestingFloor = false;
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
     */
    public void leaveCall(LocusKey locusKey) {
        // TODO we need to move away from CCS having locusKey state and where possible pass it
        // in to all methods (like we do in most cases right now)...this is temporary change in meantime
        this.locusKey = locusKey;
        leaveCall();
    }


    public void leaveCall() {
        ln.i("CallControlService.leaveCall, locus = " + locusKey);
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                // protecting this code path so it is only called by one thread at a time.
                // When you get access verify that self is still JOINED before ending call
                synchronized (leaveCallSyncLock) {
                    LocusData call = getLocusData(locusKey);
                    if (call != null && call.getLocus().getSelf() != null) {
                        LocusParticipant.State state = call.getLocus().getSelf().getState();
                        Ln.d("leaveCall, self state = " + state);
                        if (state == LocusParticipant.State.JOINED || call.getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
                            endCall(); // endCall will set self state to LEAVING

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
    public void leaveCall(String resource, boolean postLeave) {
        ln.i("CallControlService.leaveCall, resource = %s, do postLeave work = %b", resource, postLeave);
        locusService.leaveLocus(locusKey, resource);
        if (postLeave) {
            LocusData call = getLocusData(locusKey);
            sendLeaveLocusEvent(call);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(LocusLeftEvent event) {
        ln.i("CallControlService.onEvent(LocusLeftEvent)");

        LocusData call = getLocusData(locusKey);
        if (call != null) {
            ln.i("CallControlService.onEvent(LocusLeftEvent), callConnected = " + call.isCallConnected());

            // If 'auto-upload' is on and not on a debug build, then upload our logs to
            // admin service for diagnosis and analysis
            boolean uploadCallLogs = deviceRegistration.getFeatures().uploadCallLogs();
            boolean releaseBuild = !BuildConfig.DEBUG;

            if (shouldReportCallLogs(call)) {
                if (!isInCall(call)) {
                    call.setCallConnected(false);
                }

                callMetricsReporter.reportLeaveMetrics(locusKey, joinLocusTrackingID);

                if (uploadCallLogs && releaseBuild) {
                    uploadLogsService.uploadLogs(call);
                } else if (!call.wasMediaFlowing() && releaseBuild) {
                    // If some media failure occurred and we're not automatically uploading logs,
                    // then warn/prompt/'ask to auto-upload' according to setting.
                    callUi.requestUserToUploadLogs(locusKey);
                } else {
                    Ln.i("Skipping post-call upload.");
                }
            }
        }
    }

    public void getOrCreateMeetingInfo(@NonNull final LocusKey locusKey,
                                       @NonNull Action<LocusMeetingInfo> successCallback,
                                       @Nullable Action<Exception> failureCallback) {
        locusService.new GetOrCreateMeetingInfoTask(locusKey, successCallback, failureCallback).execute();
    }

    private void logStartMediaStats(Locus locus) {
        // Log locus ID, locus lastActive timestamp, and trackingID for media troubleshooting
        String callID = locus.getUniqueCallID();
        if (callID.isEmpty()) {
            Ln.i("Unable to retrieve locusID and locus lastActive.");
            callID = "";
        }
        float timezoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (float) (1000 * 3600); // in hours
        Ln.i("SQMedia Statistics for call ID - %s,%s,%.2f", callID, joinLocusTrackingID, timezoneOffset);
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


    /**
     * Cancel Call
     */
    public synchronized void cancelCall(final boolean userCancelled) {
        ln.i("CallControlService.cancelCall");
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                endMediaSession(false);
                locusService.leaveLocus(locusKey);

                LocusData call = getLocusData(locusKey);
                if (call != null) {
                    call.setCallStarted(false);
                    if (userCancelled)
                        call.setCallEndReason(new CallEndReason(CANCELLED_BY_LOCAL_USER));

                    logCall(LogCallIndex.CANCELED, call);

                    callUi.dismissRingback(locusKey);
                    stopUnansweredHandler();

                    reportCallCancelledMetrics(locusKey);

                    bus.post(new CallNotificationRemoveEvent(locusKey));
                }
                return null;
            }
        }.execute();
    }

    /**
     * Decline Call
     */
    public void declineCall(LocusKey locusKey) {
        ln.i("CallControlService.declineCall");
        LocusData locusData = getLocusData(locusKey);
        if (locusData != null) {
            locusData.setCallEndReason(new CallEndReason(CANCELLED_BY_LOCAL_USER));
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

    private void setupCallSettings() {
        // set device specific media settings (this is json string returned from WDM based on user agent info provided)
        mediaEngine.setDeviceSettings(deviceRegistration.getDeviceSettingsString());
    }

    public void muteAudio(LocusKey locusKey) {
        // Mute/un-mute audio in the local media session. (Call WME api to mute local device)
        muteAudioInMediaSession(locusKey, true);

        // signal audio mute status to the locus.
        // Call Locus api /locus/api/v1/loci/{lid}/participant/{pid}/media
        // to notify server local-mute action happened on this device.
        modifyMedia(AUDIO_TYPE, true);

        audioMutedLocally = true;
        bus.post(new CallControlLocalAudioMutedEvent(locusKey, true));
        bus.post(new CallNotificationUpdateEvent(CallNotificationType.MUTE_STATE, locusKey, true));
    }

    public void unmuteAudio(LocusKey locusKey) {
        // Un-mute audio in the local media session. (Call WME api to unmute local device)
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
            modifyMedia(AUDIO_TYPE, false);
        }

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

    /**
     * Mute audio in the local media session as a result of mute action from another user (remote muting)
     * This would also result in modify media local api call.
     */
    public void muteAudioFromRemote(LocusKey locusKey, boolean muted) {
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

        // Mute audio in the local media session. (Call WME api to mute local device)
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
                modifyMedia(VIDEO_TYPE, true);
                bus.post(new CallControlLocalVideoMutedEvent(locusKey, true, source));
                
                //lm
                Log.i("CallControlService", "muteVideo: ->end");
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
                modifyMedia(VIDEO_TYPE, false);
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

    public boolean isMeetingRecording(LocusKey locusKey) {
        LocusData locusData = getLocusData(locusKey);

        if (locusData == null)
            return false;

        Locus locus = locusData.getLocus();

        if (locus == null || locus.getControls() == null)
            return false;

        return locus.getControls().getRecord().isRecording();
    }

    public boolean isModerator(LocusKey locusKey) {
        LocusData locusData = getLocusData(locusKey);

        if (locusData == null)
            return false;

        Locus locus = locusData.getLocus();

        if (locus == null || locus.getSelf() == null)
            return false;

        return locus.getSelf().isModerator();
    }

    private void muteAudioInMediaSession(@NonNull LocusKey locusKey, boolean muted) {
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

    private void modifyMedia(final String mediaType, final boolean muted) {
        LocusData call = getLocusData(locusKey);
        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                String currentLocalSdp = mediaSession.getLocalSdp();
                String calliopeSupplementaryInformationString = buildCalliopeSupplementaryInformation();
                Map<String, Object> clusterInfo = linusReachabilityService.getLatestLinusReachabilityResults();
                Ln.d("modifyMedia, clusterInfo is " + clusterInfo);
                LocusSdp locusSdp = new LocusSdp(currentLocalSdp, MediaEngine.SDP_TYPE, calliopeSupplementaryInformationString, clusterInfo);

                if (mediaType.equals(AUDIO_TYPE)) {
                    locusSdp.setAudioMuted(muted);
                    locusSdp.setVideoMuted(mediaSession.isVideoMuted());
                } else {
                    locusSdp.setVideoMuted(muted);
                    locusSdp.setAudioMuted(mediaSession.isAudioMuted());
                }

                MediaConnection currentMediaConnection = getMediaConnection(call.getLocus());
                if (currentMediaConnection != null) {
                    List<MediaConnection> mediaConnectionList = new ArrayList<MediaConnection>();
                    MediaConnection mediaConnection = new MediaConnection();
                    mediaConnection.setType("SDP");
                    mediaConnection.setMediaUrl(currentMediaConnection.getMediaUrl());
                    mediaConnection.setMediaId(currentMediaConnection.getMediaId());
                    mediaConnection.setLocalSdp(gson.toJson(locusSdp));
                    mediaConnectionList.add(mediaConnection);

                    locusService.modifyMedia(call.getKey(), mediaConnectionList);
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
            final LocusData call = getLocusData(locusKey);
            if (call != null) {
                ln.d("Unanswered call timeout occured (timeout %d secs)", callUi.getRingbackTimeout());
                Lns.ux().i("Unanswered timeout occurred (after %d secs)", callUi.getRingbackTimeout());
                if (sdkClient.toastsEnabled()) {
                    Toaster.showLong(context, context.getString(R.string.name_was_unavailable, NameUtils.getFirstName(call.getRemoteParticipantName())));
                }
                new SafeAsyncTask<Void>() {
                    @Override
                    public Void call() throws Exception {
                        call.setCallEndReason(new CallEndReason(DIAL_TIMEOUT_REACHED));
                        endCall();
                        return null;
                    }
                }.execute();
            }
        }
    };


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ParticipantDeclinedEvent event) {
        Ln.i("CallControlService.onEvent(ParticipantDeclinedEvent)");

        LocusData call = getLocusData(locusKey);
        if (call != null) {
            Locus locus = call.getLocus();

            // Only end call when we get a decline if we're in 1:1 and the other person has declined
            if (call.onlyMeJoined() && !call.isBridge()) {
                call.setCallStarted(false);
                call.setCallEndReason(new CallEndReason(DECLINED_BY_REMOTE_USER));

                callUi.dismissRingback(locusKey);
                stopUnansweredHandler();

                endMediaSession(false);

                // for 1:1 calls, show message that other person has declined
                if (event.getReason() != null && event.getReason().equals(DeclineReason.BUSY)) {
                    if (sdkClient.toastsEnabled()) {
                        Toaster.showLong(context, R.string.call_other_person_busy);
                    }
                    logCall(LogCallIndex.BUSY, call);
                } else {
                    if (sdkClient.toastsEnabled()) {
                        Toaster.showLong(context, R.string.call_other_person_declined);
                    }
                    logCall(LogCallIndex.DECLINED, call);
                }

                locusService.leaveLocus(event.getLocusKey());
                bus.post(CallControlLeaveLocusEvent.callDeclined(call));
            }
        }
    }

    public void copyCall() {
        Ln.i("CallControlService.copyCall");

        // end current media session, for new start in join
        endMediaSession(false);

        LocusData call = getLocusData(locusKey);
        if (call != null) {
            call.setMediaSession(null);

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
                joinCall(callContext);

                // start copied call in muted state
                MediaSession mediaSession = call.getMediaSession();
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
                        .setIsOneOnOne(call.isOneOnOne())
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
                ln.v("%s,   %s", methodTag, participant.getPerson());
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

            // The LocusKey from the event should be used for work within this method, but it should NOT update this.locusKey yet!
            LocusKey localLocusKey = event.getLocusKey();
            LocusData call = getLocusData(event.getLocusKey());
            if (call != null) {
                Locus locus = call.getLocus();
                int numberJoinedParticipants = locus.getFullState().getCount();

                if (locusDataCache.isInCall(localLocusKey)) { // only handle this event if we're actually joined on this device

                    callNotification.dismiss(localLocusKey);

                    // check if we're in observing state (joined but with media on room endpoint)
                    boolean isObserving = call.isObserving(deviceRegistration.getUrl());
                    Ln.i("%s, joined on this device, observing = %b, mediaStarted = %b", methodTag, isObserving, isMediaStarted(locusKey));

                    if (!isObserving) {
                        // if not in observing state then start local media
                        if (!isMediaStarted(locusKey)) {
                            if (callContext != null) {
                                call.setAudioCall(callContext.isAudioCall());
                            }
                            startMedia(call);
                            call.setActiveSpeakerId(null);
                        }
                    } else {
                        if (!isMediaStarted(locusKey)) {
                            startMedia(call);
                            setRoomJoined();
                        }
                    }

                    if (!call.isCallStarted()) {
                        call.setCallStarted(true);

                        if (isObserving) {
                            setRoomCallNotifying();
                        }
                    }

                    // check if we're now "connected" i.e. other participant(s) now joined (note that room counts as participant
                    // when we're in observing data). Alternatively we could be in the lobby of the webex meeting waiting for the host
                    // and need to check for that
                    int callConnectParticipantCount = call.isObservingNotMoving(deviceRegistration.getUrl()) ? 3 : 2;
                    if (numberJoinedParticipants >= callConnectParticipantCount || call.getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
                        if (sdkClient.toastsEnabled()) {
                            Toaster.showShort(context, R.string.call_participant_joined);
                        }

                        // callConnected flag is only used when we have media on local device (i.e.
                        if (!isObserving && !call.isCallConnected()) {

                            call.setCallConnected(true);
                            call.setEpochStartTime(new Date().getTime());
                            bus.post(new CallControlCallConnectedEvent(localLocusKey));

                            handleMediaDeviceRegistration();

                            callMetricsReporter.reportJoinMetrics(localLocusKey);
                            logCall(LogCallIndex.CONNECTED, call);
                        }


                        if (isObserving) {
                            String remoteParticipantName = call.getRemoteParticipantName();
                            // TODO check this logic....is here to handle case where calling sip endpoint....can we check for no covnersation url?
                            String conversationTitle = locus.getParticipants().size() > 3 ? null : remoteParticipantName; // passing null will keep the original name of the room.
                            setRoomCallConnected(conversationTitle);
                            logCall(LogCallIndex.CONNECTED, call);
                        }


                        callUi.dismissRingback(localLocusKey);
                        stopUnansweredHandler();

                        // Make sure whiteboard share flag is correct due to Floor event can't be received when call is left in current design.
                        // Need to work with Server team to see if they can send this event in this case
                        if (call.isFloorGranted()) {
                            if (locus.getWhiteboardMedia() != null) {
                                locusService.setSharingWhiteboard(true);
                            } else {
                                locusService.setSharingWhiteboard(false);
                            }
                        }
                        bus.post(new CallControlParticipantJoinedEvent(localLocusKey, event.getJoinedParticipants()));
                        bus.post(new CallNotificationEvent(CallNotificationType.ONGOING, localLocusKey, call.isOneOnOne(), isMediaStarted(locusKey)));
                    } else {
                        if (call.isOneOnOne() && !call.getLocus().isJoinedFromOtherDevice(deviceRegistration.getUrl())) {
                            callUi.startRingback(locusKey);
                            startUnansweredHandler();
                        }

                        bus.post(new CallControlLocusCreatedEvent(localLocusKey));

                        sendCallNotificationEvent(localLocusKey, call);
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
        LocusData call = getLocusData(event.getLocusKey());

        if (call != null) {
            Locus locus = call.getLocus();
            int remainingParticipants = locus.getFullState().getCount();

            // check if this is indication that user cancelled call
            if (call.isOneOnOne() && remainingParticipants == 0 && locus.getSelf() != null && locus.getSelf().getState().equals(LocusParticipant.State.NOTIFIED)) {
                if (sdkClient.toastsEnabled()) {
                    Toaster.showLong(context, R.string.call_participant_cancelled);
                }
                bus.post(new CallControlCallCancelledEvent(call.getKey()));
            } else {

                if (isInCall(call)) {
                    int self = call.isObserving(deviceRegistration.getUrl()) ? 2 : 1;
                    if (call.isOneOnOne() && remainingParticipants == self && !call.isMeeting()) {
                        // One-on-one and we are still in the call
                        if (sdkClient.toastsEnabled()) {
                            Toaster.showLong(context, R.string.call_participant_left);
                        }
                        call.setCallEndReason(new CallEndReason(ENDED_BY_REMOTE_USER));
                        leaveCall();
                    }
                }
                bus.post(new CallControlParticipantLeftEvent(call.getKey(), event.getLeftParticipants()));
            }
        } else {
            ln.i("%s, no call found for key", methodTag);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(FirstAudioPacketReceivedEvent event) {
        Ln.i("CallControlService.onEvent(FirstAudioPacketReceivedEvent)");
        callUi.dismissRingback(locusKey);
    }

    private boolean isInCall(LocusData call) {
        return locusDataCache.isInCall(call.getKey());
    }

    protected boolean isInPairedCall(LocusData call) {
        return call.isObserving(deviceRegistration.getUrl());
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(AnsweredInactiveCallEvent event) {
        Ln.i("CallControlService.onEvent(AnsweredInactiveCallEvent)");
        LocusData call = getLocusData(event.getLocusKey());
        if (call != null) {
            endMediaSession(false);
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
        LocusData call = getLocusData(event.getLocusKey());
        if (call != null) {
            call.setCallEndReason(new CallEndReason(ENDED_BY_LOCUS));
            if (call.isCallStarted()) {
                endCall();
            }
            if (call.getLocus().getSelf().getReason() == LocusParticipant.Reason.FORCED) {
                Toaster.showLong(context, R.string.call_participant_expelled);
            }
        }

        if (call.getLocus().getSelf().getReason() == LocusParticipant.Reason.FORCED) {
            Toaster.showLong(context, R.string.call_participant_expelled);
            bus.post(new CallControlMeetingControlsExpelEvent(event.getLocusKey(), true));
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

    private void startMedia(LocusData call) {
        MediaConnection mediaConnection = getMediaConnection(call.getLocus());
        if (mediaConnection != null) {
            String remoteSdpString = mediaConnection.getRemoteSdp();
            LocusSdp remoteSdp = gson.fromJson(remoteSdpString, LocusSdp.class);

            ln.d("startMedia, callContext = " + callContext);
            if (callContext != null && callContext.getMediaSession() != null && call.getMediaSession() == null) {
                Ln.d("Setting MediaSession for locus = %s", call.getKey());
                call.setMediaSession(callContext.getMediaSession());
            }

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                mediaSession.startMedia();

                String sdp = remoteSdp.getSdp();
                if (sdp != null && !sdp.isEmpty()) {

                    // check if wme feature toggles are present in self device info
                    Map<String, String> featureToggles = null;
                    LocusSelfRepresentation self = call.getLocus().getSelf();
                    if (self.getDevices().size() > 0) {
                        featureToggles = self.getDevices().get(0).getFeatureToggles();
                    }

                    mediaSession.answerReceived(sdp, featureToggles);
                }
                logStartMediaStats(call.getLocus());
            }
        }
    }

    private void updateMedia(LocusData call) {
        MediaConnection mediaConnection = getMediaConnection(call.getLocus());
        if (mediaConnection != null) {
            String remoteSdpString = mediaConnection.getRemoteSdp();
            LocusSdp remoteSdp = gson.fromJson(remoteSdpString, LocusSdp.class);

            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {

                String sdp = remoteSdp.getSdp();
                if (sdp != null && !sdp.isEmpty()) {
                    mediaSession.updateSDP(sdp);
                }
            }
        }
    }


    private void logCall(String msg, LocusData call) {
        logFilePrint.getCallIndex().addCall(msg, call, trackingIdGenerator.currentTrackingId());
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
        LocusData call = getLocusData(locusKey);
        if (call != null) {
            ln.d("CallControlService.onEventMainThread(MediaUpdatedEvent) mediaSessionInEndingState ? %s isMediaStarted ? %s", mediaSessionInEndingState(locusKey), isMediaStarted(locusKey));
            if (!mediaSessionInEndingState(locusKey)) {
                if (!isMediaStarted(locusKey)) {
                    ln.d("CallControlService.onEventMainThread(MediaUpdatedEvent) self state = %s %s", call.getLocus().getSelf().getState(), MediaSessionUtils.toString(getMediaSession(locusKey)));
                    startMedia(call);
                } else {
                    updateMedia(call);
                }
            }
        } else {
            ln.w("Could not find Locus DTO for key: %s", locusKey);
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
        LocusData call = getLocusData(locusKey);
        if (call != null && !isMediaStarted(locusKey))
            startMedia(call);
        else
            ln.w("Could not find Locus DTO for key: %s", locusKey);

        if (requestingFloor) {
            //Media is created so we can finally start the share
            locusService.shareScreen(locusKey);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(FloorRequestAcceptedEvent event) {
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null && !mediaSession.isScreenSharing() && event.isContent()) {
                mediaSession.startScreenShare(screenShareCallback, call.getFloorGrantedId());
            }
        }
        requestingFloor = false;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(FloorRequestDeniedEvent event) {
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            if (isInPairedCall(call) && !call.isFloorGranted() && event.isContent() && call.getMediaSession() != null) {
                call.getMediaSession().endSession();
                locusService.deleteMedia(locusKey, getMediaConnection(call.getLocus()));
            }
        }
        requestingFloor = false;
    }

    /**
     * Notification from media engine that network congestion has been detected.  In response
     * we disable sending video and instruct other endpoint to do likewise
     */
    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(NetworkCongestionEvent event) {
        Ln.i("CallControlService.onEvent(NetworkCongestionEvent)");

        if (sdkClient.toastsEnabled()) {
            Toaster.showLong(context, R.string.poor_network_quality);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(NetworkDisableVideoEvent event) {
        Ln.i("CallControlService.onEvent(NetworkDisableVideoEvent)");

        if (sdkClient.toastsEnabled()) {
            Toaster.showLong(context, R.string.video_disabled_due_to_network_conditions);
        }
        Ln.w("Disable sending video due to poor network conditions");
        LocusData locusData = getLocusData(locusKey);
        locusData.setVideoDisabled(true);

        muteVideo(locusKey, MediaRequestSource.NETWORK);

        // notify UI that video has been disabled
        bus.post(new CallControlDisableVideoEvent());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkDisconnectEvent event) {
        Ln.i("CallControlService.onEvent(NetworkDisconnectEvent)");
        locusService.callFlowTrace("App", "WME", "NetworkDisconnectEvent: pause call", locusKey);
        bus.post(new CallControlDisconnectedEvent());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkReconnectEvent event) {
        Ln.i("CallControlService.onEvent(NetworkReconnectEvent)");
        locusService.callFlowTrace("App", "WME", "NetworkReconnectEvent: resume call", locusKey);
        bus.post(new CallControlReconnectEvent());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkLostEvent event) {
        Ln.i("CallControlService.onEvent(NetworkLostEvent)");
        locusService.callFlowTrace("App", "WME", "NetworkLostEvent: end call", locusKey);
        bus.post(new CallControlLostEvent());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(DeviceCameraUnavailable event) {
        Ln.w("CallControlService.onEvent(DeviceCameraUnavailable)");
        if (sdkClient.toastsEnabled()) {
            Toaster.showLong(context, R.string.camera_unavailable);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(StunTraceResultEvent event) {
        Ln.w("CallControlService.onEvent(StunTraceResultEvent)");

        LocusData locusData = getLocusData(locusKey);
        if (locusData != null && deviceRegistration.getFeatures().isTeamMember()) {
            callMetricsReporter.reportCallStunTraceMetrics(locusKey, event.getDetail());
        }
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ICEConnectionFailedEvent event) {
        Ln.w("CallControlService.onEvent(ICEConnectionFailedEvent)");
        LocusData call = getLocusData(locusKey);
        if (call != null)
            call.setCallEndReason(new CallEndReason(CANCELLED_BY_LOCAL_ERROR, "ICE connection failure"));
        cancelCall(false);

        callUi.reportIceFailure(locusKey);

        bus.post(new CallControlCallJoinErrorEvent());
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ErrorJoiningLocusEvent event) {
        Ln.i("CallControlService.onEvent(ErrorJoiningLocusEvent)");
        handleJoinError(R.string.call_error_joining, event.getErrorMessage(), event.getErrorCode());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(ConflictErrorJoiningLocusEvent event) {
        Ln.i("CallControlService.onEvent(ConflictErrorJoiningLocusEvent)");
        handleJoinError(R.string.call_error_joining_conflict, event.getErrorMessage(), event.getErrorCode());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(HighVolumeErrorJoiningLocusEvent event) {
        Ln.i("CallControlService.onEvent(HighVolumeErrorJoiningLocusEvent)");
        handleJoinError(R.string.call_error_joining_high_volume, event.getErrorMessage(), event.getErrorCode());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(LocusInviteesExceedMaxSizeEvent event) {
        Ln.i("CallControlService.onEvent(LocusInviteesExceedMaxSizeEvent), code = " + event.getErrorCode());

        // Indication that we exceeded max number of invitees allowed when this participant initiates or adds guests to a meeting. (403)

        String message = "";
        int maxRosterSize = deviceRegistration.getFeatures().getMaxRosterSize();
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

        if (!message.isEmpty() && sdkClient.toastsEnabled()) {
            Toaster.showLong(context, message);
        }

        if (event.isJoin()) {
            endMediaSession(false);
            bus.post(new CallControlCallJoinErrorEvent());
        }
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(LocusMeetingLockedEvent event) {
        Ln.i("CallControlService.onEvent(LocusMeetingLockedEvent), code = " + event.getErrorCode());

        // Indication that we received a locked (423) response from Locus.

        String message = "";
        int maxBridgeSize = deviceRegistration.getFeatures().getMaxBridgeSize();
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
                    break;
                case LocusRequiresModeratorPINorGuestPIN:
                    break;
            }
        } else {
            message = context.getString(R.string.call_error_joining);
        }

        if (!message.isEmpty() && sdkClient.toastsEnabled()) {
            Toaster.showLong(context, message);
        }

        if (event.isJoin()) {
            endMediaSession(false);
            bus.post(new CallControlCallJoinErrorEvent());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(JoinedLobbyEvent event) {
        Ln.i("CallControlService.onEvent(JoinedLobbyEvent");
        LocusKey locusKey = event.getLocusKey();
        if (locusKey == null) {
            Toaster.showLong(context, context.getString(R.string.call_error_joining));
            return;
        }

        if (mediaEngine.getActiveMediaSession() != null) {
            mediaEngine.getActiveMediaSession().endSession();
        }

        startLobbyKeepAlive(locusKey);
        callUi.showMeetingLobbyUi(event.getLocusKey());
        bus.post(new CallControlJoinedLobbyEvent(event.getLocusKey()));
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(JoinedMeetingEvent event) {
        Ln.i("CallControlService.onEvent(JoinedMeetingEvent, LocusKey=" + event.getLocusKey());

        // TODO: A user could be on another call while waiting in the lobby. Need to handle this scenario just as when a call is incoming while on another call. for now, just exit.
        LocusKey activeLocusKey = locusDataCache.getActiveLocus();
        if (activeLocusKey != null && !activeLocusKey.equals(event.getLocusKey())) {
            Ln.w("CallControlService.onEvent(JoinedMeetingEvent):" +
                            "LocusKey %s in the event does not match the currently active LocusKey of %s",
                    event.getLocusKey().toString(),
                    this.locusKey.toString());
            return;
        }

        if (locusService.isJoiningLocus()) {
            Ln.i("CallControlService.onEvent(JoinedMeetingEvent): Exiting. Already joining Locus " + event.getLocusKey());
            return;
        }

        CallContext updatedCallContext = new CallContext.Builder(event.getLocusKey())
                .setIsAnsweringCall(false)
                .setIsOneOnOne(false)
                .setShowFullScreen(true)
                .setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioVideoShare)
                .setUseRoomPreference(CallContext.UseRoomPreference.DontUseRoom)
                .setCallInitiationOrigin(CallInitiationOrigin.CallOriginationToast).build();

        bus.post(new CallControlJoinedMeetingEvent(event.getLocusKey()));
        joinMeeting(updatedCallContext);
    }

    private void handleJoinError(int messageId, String errorMessage, int errorCode) {
        if (sdkClient.toastsEnabled()) {
            Toaster.showLong(context, messageId);
        }
        endMediaSession(false);
        bus.post(new CallControlCallJoinErrorEvent());
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventBackgroundThread(LocusDataCacheChangedEvent event) {
        ln.i("CallControlService.onEvent(LocusDataCacheChangedEvent): %s was %s", event.getLocusKey().toString(), event.getLocusChange().toString());
        updateLocusKeyAndActiveCall(event.getLocusKey());
        if (!locusDataCache.isCallActive(locusKey)) {
            isCaller = false;
        }
    }

    @SuppressWarnings("UnusedDeclaration") // called by the event bus.
    public void onEventBackgroundThread(LocusDataCacheReplacesEvent event) {
        ln.i("CallControlService.onEvent(LocusDataCacheReplacesEvent: %s with %s)", event.getReplacedLocusKey(), event.getLocusKey());
        if (locusKey.equals(event.getReplacedLocusKey()))
            updateLocusKeyAndActiveCall(event.getLocusKey());
        else
            ln.w(false, "CallControlService.onEvent(LocusDataCacheReplacesEvent) LocusKey does not match current key: %s", locusKey);
    }

    private void updateLocusKeyAndActiveCall(LocusKey newLocusKey) {
        // Update local DB flag of Locus joined state
        LocusData call = locusDataCache.getLocusData(newLocusKey);
        if (call != null) {
            if (locusKey == null) {
                locusKey = newLocusKey;
            } else if (!locusKey.equals(newLocusKey) && locusKeyIsReplacement(call)) {
                locusKey = newLocusKey;
            }
            setActiveCall(newLocusKey, call.getLocus().getFullState().isActive());
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
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            // check if new CSI (capture source identifier) represents a video source and, if so,
            // check if it matches CSI for one of the participants in roster
            Locus locus = call.getLocus();
            if (event.videoChanged()) {
                boolean foundActiveSpeaker = false;
                for (LocusParticipant locusParticipant : locus.getParticipants()) {
                    List<Long> participantCSIs = locusParticipant.getStatus().getCsis();
                    for (int i = 0; i < event.getNewCSIs().length; i++) {
                        long activeCSI = event.getNewCSIs()[i];
                        if (participantCSIs.contains(activeCSI)) {
                            Ln.d("CallControlService.onEvent(MediaActiveSpeakerChangedEvent), active speaker = " + locusParticipant.getPerson().getDisplayName());
                            call.setActiveSpeakerId(locusParticipant.getId());
                            bus.post(new CallControlActiveSpeakerChangedEvent(call, locusParticipant, event.getVideoId()));
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

        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            Long mutedCsi = event.getCsi();

            LocusParticipant participant = findParticipantFromCsi(call.getLocus().getParticipants(), mutedCsi);
            if (participant == null) { // this will potentially be the case when getting unmuted notification from linus
                participant = call.getActiveSpeaker();
            }

            if (participant != null) {
                bus.post(new CallControlParticipantVideoMutedEvent(locusKey, participant, event.isMuted()));
            }
        }
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
    public void onEvent(FloorGrantedEvent event) {
        ln.i("CallControlService.onEvent(FloorGrantedEvent): %s", locusKey);
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            /* TODO: 9/2/2016 Decided to disable joining call and viewing shared screen (on mobile devices) until
                (a) the feature is deemed desirable; (b) UI/UX is defined; and (c) testing resources are available
            if (!locusDataCache.isInCall() && locusDataCache.isZtmCall(locusKey)) {
                CallContext callContext = new CallContext.Builder(locusKey)
                        .setIsOneOnOne(call.isOneOnOne())
                        .setIsAnsweringCall(true)
                        .setShowFullScreen(false)
                        .setMediaDirection(MediaEngine.MediaDirection.SendReceiveShareOnly)
                        .setUseRoomPreference(CallContext.UseRoomPreference.DontUseRoom)
                        .setCallInitiationOrigin(CallInitiationOrigin.CallOriginationZTM).build();
                ln.i("CallControlService.onEvent(FloorGrantedEvent), join ztm call for share");
                joinCall(callContext);

                MediaShare mediaShare = call.getLocus().getGrantedFloor();
                if (mediaShare.isContent()) {
                    bus.post(new CallControlViewDesktopShare(locusKey));
                } else if (mediaShare.isWhiteboard()) {
                    locusService.setSharingWhiteboard(true);
                    bus.post(new CallControlViewWhiteboardShare(locusKey));
                }

            } else */ if (!locusDataCache.isInCall()) {
                ln.i("CallControlService.onEvent(FloorGrantedEvent), not joined from this device, so ignore");
                return;

            } else if (isInPairedCall(call)) {
                startMediaSession(this, MediaEngine.MediaDirection.SendReceiveShareOnly);
            }

            MediaShare share = call.getLocus().getGrantedFloor();
            if ((share != null) && (share.isWhiteboard())) {
                locusService.setSharingWhiteboard(true);
                fireWhiteboardShareEvent(call);
            }
            bus.post(new CallControlFloorGrantedEvent(locusKey));

            // If we join the call (ztm) media session is set on callContext in joinCall
            MediaSession mediaSession = null;
            if (call.getMediaSession() != null) {
                mediaSession = call.getMediaSession();
            } else if (callContext != null) {
                mediaSession = callContext.getMediaSession();
            }

            if (mediaSession != null && call.getLocus() != null) {

                MediaShare mediaShare = call.getLocus().getGrantedFloor();
                if (mediaShare == null) {
                    ln.w("CallControlService.onEvent(FloorGrantedEvent), null media share");
                    return;
                }

                if (mediaShare.isContent()) {
                    mediaSession.updateShareId(call.getFloorGrantedId());
                }

                if (!call.isFloorMine()) {
                    if (mediaSession.isScreenSharing()) {
                        mediaSession.stopScreenShare(call.getFloorGrantedId());
                    }

                    ln.i("CallControlService.onEvent(FloorGrantedEvent), join share");
                    if (mediaShare.isContent())
                        mediaSession.joinShare(call.getFloorGrantedId());
                    else if (mediaShare.isWhiteboard()) {
                        locusService.setSharingWhiteboard(true);
                        fireWhiteboardShareEvent(call);
                    }
                } else {
                    mediaSession.leaveShare(call.getFloorGrantedId());
                }
            } else {
                ln.w("CallControlService.onEvent(FloorGrantedEvent), unable to join share, no media session");
            }
        }
    }

    private void fireWhiteboardShareEvent(LocusData call) {
        Uri whiteboardUrl = getWhiteboardMediaShareUrlFromLocus(call);
        if (whiteboardUrl != null) {
            bus.post(new CallControlViewWhiteboardShare(locusKey, whiteboardUrl));
        }
    }

    private Uri getWhiteboardMediaShareUrlFromLocus(LocusData locus) {
        ////TODO: find a more elegant way to do the chained null check
        if (locus == null
                || locus.getLocus() == null
                || locus.getLocus().getWhiteboardMedia() == null
                || locus.getLocus().getWhiteboardMedia().getUrl() == null) {

            return null;
        }

        return locus.getLocus().getWhiteboardMedia().getUrl();
    }


    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(FloorReleasedEvent event) {
        ln.i("CallControlService.onEvent(FloorReleasedEvent): %s", event.getLocusKey());
        LocusData call = locusDataCache.getLocusData(event.getLocusKey());
        if (call != null && locusDataCache.isInCall()) {
            bus.post(new CallControlFloorReleasedEvent(event.getLocusKey()));

            //
            MediaSession mediaSession = call.getMediaSession();
            if (mediaSession != null) {
                MediaShare mediaShare = call.getLocus().getReleasedFloor();
                if (mediaShare.isContent()) {
                    mediaSession.updateShareId("");
                }
            }

            /* TODO: 9/2/2016 Decided to disable joining call and viewing shared screen (on mobile devices) until
                (a) the feature is deemed desirable; (b) UI/UX is defined; and (c) testing resources are available
            if (locusKey != null && locusKey.equals(event.getLocusKey()) && locusDataCache.isZtmCall(locusKey)) {
                leaveCall();
            } else */ {
                if (mediaSession != null) {
                    mediaSession.leaveShare(call.getFloorGrantedId());

                    if (mediaSession.isScreenSharing()) {
                        mediaSession.stopScreenShare(call.getFloorGrantedId());
                    } else {
                        Uri whiteboardUrl = getWhiteboardMediaShareUrlFromLocus(call);
                        if (whiteboardUrl != null) {
                            locusService.setSharingWhiteboard(false);
                            bus.post(new CallControlEndWhiteboardShare(locusKey, whiteboardUrl));
                        }
                    }

                    if (isInPairedCall(call)) {
                        mediaSession.endSession();  // TODO should we also need to call mediaSession.stopMedia() as well?
                        locusService.deleteMedia(locusKey, getMediaConnection(call.getLocus()));
                    }
                }
            }
        } else {
            ln.i("CallControlService.onEvent(FloorReleasedEvent): ignored because call is null, or not joined from this device");
        }
    }

    public LocusData getLocusData(LocusKey locusKey) {
        return locusService.getLocusData(locusKey);
    }

    public String getCallOrigin() {
        if (callContext != null) {
            CallInitiationOrigin origin = callContext.getCallInitiationOrigin();
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
        endMediaSession(false);
        bus.post(new CallControlCallJoinErrorEvent());
        bus.post(new CallControlInvalidLocusEvent(event.getErrorCode(), event.getInvitee()));
    }

    @Nullable
    private MediaSession getMediaSession(LocusKey locusKey) {
        MediaSession mediaSession = null;
        if (locusKey != null) {
            LocusData call = getLocusData(locusKey);
            if (call != null) {
                mediaSession = call.getMediaSession();
            }
        }
        // at call initiation time we may not have LocusData instance at this point
        // e.g. when needing to start self preview.
        if (mediaSession == null && callContext != null) {
            mediaSession = callContext.getMediaSession();
        }
        return mediaSession;
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
        if (locusKey != null && locusDataCache.exists(locusKey) && getLocusData(locusKey).getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
            if (lobbyKeepAliveTimer != null) {
                lobbyKeepAliveTimer.cancel();
                lobbyKeepAliveTimer.purge();
                lobbyKeepAliveTimer = null;
            }

            LocusParticipantDevice currentDevice = locusService.getCurrentDevice(locusDataCache.getLocusData(locusKey));
            int keepAliveInSeconds = (currentDevice.getKeepAliveSecs() / 2);
            int keepAliveInMillis = keepAliveInSeconds * 1000;
            if (keepAliveInSeconds > 0) {
                ln.d("CallControlService.startLobbyKeepAlive, starting Lobby keep-alive for PMR %s with value of %s seconds.", locusKey.toString(), keepAliveInSeconds);
                lobbyKeepAliveTimer = new Timer();
                lobbyKeepAliveTimer.schedule(new LobbyKeepAliveTimerTask(locusKey), keepAliveInMillis, keepAliveInMillis);
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
        LocusData call = getLocusData(locusKey);
        if (call != null) {
            call.setAudioCall(!call.isAudioCall());

            if (call.isAudioCall()) {
                updateMediaSession(MediaEngine.MediaDirection.SendReceiveAudioOnly);
            } else {
                updateMediaSession(MediaEngine.MediaDirection.SendReceiveAudioVideoShare);
            }

            callMetricsReporter.reportCallAudioToggleMetrics(locusKey, call.isAudioCall());

            bus.post(new CallControlAudioOnlyStateChangedEvent(locusKey));
        }
    }

    // TODO if we end up getting some sort of meeting type from Locus in the future, remove use of this
    public boolean isCaller() {
        return isCaller;
    }


    // These methods are implemented by each of the clients

    // Default base implementation
    public boolean shouldReportCallLogs(LocusData call) {
        return call.isCallConnected();
    }

    // Default base implementation
    public boolean isJoiningCall() {
        return locusService.isJoiningLocus();
    }

    public void requestAudioFocus(MediaEngine.MediaDirection mediaDirection) { }

    public void abandonAudioFocus() { }

    public void handleRoomCallEnd() { }

    public void sendLeaveLocusEvent(LocusData call) { }

    public void reportCallCancelledMetrics(LocusKey locusKey) { }

    public void updateRoomCopyCall(boolean isObserving, RoomUpdatedEvent.RoomState roomState, boolean localMedia) { }

    public void handleMediaDeviceRegistration() { }

    public void setRoomJoined() { }

    public void setRoomCallNotifying() { }

    public void setRoomCallConnected(String conversationTitle) { }

    public void sendCallNotificationEvent(LocusKey localLocusKey, LocusData call) { }
}
