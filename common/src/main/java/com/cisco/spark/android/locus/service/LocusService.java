package com.cisco.spark.android.locus.service;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.acl.Acl;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsExpelEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsLockEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantAudioMuteEvent;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.AccessManager;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.locus.events.AnsweredInactiveCallEvent;
import com.cisco.spark.android.locus.events.ConflictErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.ErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.FloorRequestAcceptedEvent;
import com.cisco.spark.android.locus.events.FloorRequestDeniedEvent;
import com.cisco.spark.android.locus.events.HighVolumeErrorJoiningLocusEvent;
import com.cisco.spark.android.locus.events.InvalidLocusEvent;
import com.cisco.spark.android.locus.events.LocusInviteesExceedMaxSizeEvent;
import com.cisco.spark.android.locus.events.LocusLeftEvent;
import com.cisco.spark.android.locus.events.LocusMeetingLockedEvent;
import com.cisco.spark.android.locus.events.LocusUrlUpdatedEvent;
import com.cisco.spark.android.locus.events.NewLocusEvent;
import com.cisco.spark.android.locus.events.ParticipantJoinedEvent;
import com.cisco.spark.android.locus.events.ParticipantSelfChangedEvent;
import com.cisco.spark.android.locus.events.RetrofitErrorEvent;
import com.cisco.spark.android.locus.model.Floor;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusLink;
import com.cisco.spark.android.locus.model.LocusMeetingInfo;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantDevice;
import com.cisco.spark.android.locus.model.LocusParticipantInfo;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.model.MediaConnection;
import com.cisco.spark.android.locus.model.MediaDirection;
import com.cisco.spark.android.locus.model.MediaShare;
import com.cisco.spark.android.locus.requests.AlertLocusRequest;
import com.cisco.spark.android.locus.requests.CallLocusRequest;
import com.cisco.spark.android.locus.requests.CreateAclRequest;
import com.cisco.spark.android.locus.requests.DeclineLocusRequest;
import com.cisco.spark.android.locus.requests.FloorShareRequest;
import com.cisco.spark.android.locus.requests.JoinLocusRequest;
import com.cisco.spark.android.locus.requests.LeaveLocusRequest;
import com.cisco.spark.android.locus.requests.LocusControlRequest;
import com.cisco.spark.android.locus.requests.LocusHoldRequest;
import com.cisco.spark.android.locus.requests.LocusInvitee;
import com.cisco.spark.android.locus.requests.LocusResumeRequest;
import com.cisco.spark.android.locus.requests.MediaCreationRequest;
import com.cisco.spark.android.locus.requests.MergeLociRequest;
import com.cisco.spark.android.locus.requests.MigrateRequest;
import com.cisco.spark.android.locus.requests.ModifyMediaRequest;
import com.cisco.spark.android.locus.requests.SendDtmfRequest;
import com.cisco.spark.android.locus.requests.UpdateLocusRequest;
import com.cisco.spark.android.locus.responses.CreateAclResponse;
import com.cisco.spark.android.locus.responses.GetLocusListResponse;
import com.cisco.spark.android.locus.responses.JoinLocusResponse;
import com.cisco.spark.android.locus.responses.LeaveLocusResponse;
import com.cisco.spark.android.locus.responses.LocusParticipantResponse;
import com.cisco.spark.android.locus.responses.LocusResponse;
import com.cisco.spark.android.locus.responses.MediaCreationResponse;
import com.cisco.spark.android.locus.responses.MediaDeletionResponse;
import com.cisco.spark.android.locus.responses.MergeLociResponse;
import com.cisco.spark.android.locus.responses.ModifyMediaResponse;
import com.cisco.spark.android.locus.responses.UpdateLocusResponse;
import com.cisco.spark.android.log.Lns;
import com.cisco.spark.android.media.MediaSessionUtils;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.mercury.events.DeclineReason;
import com.cisco.spark.android.mercury.events.LocusChangedEvent;
import com.cisco.spark.android.mercury.events.LocusDeltaEvent;
import com.cisco.spark.android.mercury.events.LocusEvent;
import com.cisco.spark.android.metrics.CallMetricsReporter;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.model.UserEmailRequest;
import com.cisco.spark.android.model.UserIdentityKey;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.Sanitize;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;


import static com.cisco.spark.android.sync.ConversationContentProviderOperation.clearConversationInActiveCall;

/**
 * Provides access to locus call signalling api (apidocs.wbx2.com/locus/html/#Locus_Service_-_V1).
 * <p/>
 * The current version is based on a subset of these APIs that support "permanent locus" (the same locus is associated
 * with every conversation as opposed to previous approach where new transient (ephemeral) locus was created for every call).
 * See following for examples of permanent locus usage: http://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Permanent-Locus-API-examples
 * Following shows sample call flows: http://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Locus-Callflows
 */
public class LocusService implements Component {
    public final static String LOCUS_URL = "LS_LOCUS_URL";

    private final Context context;
    protected EventBus bus;
    private final DeviceRegistration deviceRegistration;
    private final ApiClientProvider apiClientProvider;
    private final LocusDataCache locusDataCache;
    private final LocusProcessor locusProcessor;
    private final TrackingIdGenerator trackingIdGenerator;
    private final AccessManager accessManager;
    private final CallMetricsReporter callMetricsReporter;
    private final Gson gson;
    private final ApiTokenProvider apiTokenProvider;
    private final SchedulerProvider schedulerProvider;
    private final Lazy<EncryptedConversationProcessor> conversationProcessorLazy;

    private final NaturalLog ln = Ln.get("$LOCUSSERVICE");

    private boolean joiningLocus = false;
    private LocusKey joiningLocusKey;
    private boolean cancelJoin = false;

    private String yearUtcTimezoneOffset = "2014, 0";
    private boolean sharingWhiteboard;

    public LocusService(Context context, EventBus bus, DeviceRegistration deviceRegistration,
                        ApiClientProvider apiClientProvider, LocusDataCache locusDataCache,
                        LocusProcessor locusProcessor,
                        TrackingIdGenerator trackingIdGenerator,
                        AccessManager accessManager,
                        CallMetricsReporter callMetricsReporter, Gson gson,
                        ApiTokenProvider apiTokenProvider,
                        SchedulerProvider schedulerProvider,
                        Lazy<EncryptedConversationProcessor> conversationProcessorLazy) {
        this.context = context;
        this.bus = bus;
        this.deviceRegistration = deviceRegistration;
        this.apiClientProvider = apiClientProvider;
        this.locusDataCache = locusDataCache;
        this.locusProcessor = locusProcessor;
        this.trackingIdGenerator = trackingIdGenerator;
        this.accessManager = accessManager;
        this.callMetricsReporter = callMetricsReporter;
        this.gson = gson;
        this.apiTokenProvider = apiTokenProvider;
        this.schedulerProvider = schedulerProvider;
        this.conversationProcessorLazy = conversationProcessorLazy;
        bus.register(this);
    }

    public void updateLocusWithMeetingPin(final LocusKey locusKey, final String hostPin) {

        if (!joiningLocus)  {
            joiningLocus = true;
            joiningLocusKey = locusKey;

            updateYearUtcTimezoneOffset();

            // TODO: Setting this minimal number of properties on the JoinLocusRequest seems sufficient.
            JoinLocusRequest joinLocusRequest = new JoinLocusRequest();
            joinLocusRequest.setPin(hostPin);
            joinLocusRequest.setModerator(true);
            if (deviceRegistration.getFeatures().isNativeClientLobbyEnabled()) {
                joinLocusRequest.setSupportsNativeLobby(true);
            }

            String url = locusKey.getUrl().toString() + "/participant";
            apiClientProvider.getHypermediaLocusClient().joinLocus(url, joinLocusRequest).enqueue(new retrofit2.Callback<JoinLocusResponse>() {
                @Override
                public void onResponse(Call<JoinLocusResponse> call, retrofit2.Response<JoinLocusResponse> response) {
                    joiningLocus = false;
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [updateLocusWithMeetingPin]", locusKey, getTrackingId(response));
                    } else {
                        callFlowTrace("Locus", "App", "failure() [updateLocusWithMeetingPin]", locusKey, getTrackingId(response));
                        handleLocusError(response, true);
                    }
                }

                @Override
                public void onFailure(Call<JoinLocusResponse> call, Throwable t) {
                    joiningLocus = false;
                    cancelJoin = false;
                    Ln.i(t, "LocusService, updateLocusWithMeetingPin, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });

            callFlowTrace("App", "Locus", "[updateLocusWithMeetingPin]", locusKey);
        } else {
            Ln.i("LocusService.updateLocusWithMeetingPin, currently joining call");
        }
    }


    /**
     * Join Locus (with media)
     * https://apidocs.wbx2.com/locus/html/api-TG9jdXMgU2VydmljZSAtIFYxX0pvaW4gTG9jdXM=.html
     * @param locusKey locus to join
     * @param mediaConnectionList local audio/video media information
     */
    public void joinLocus(final LocusKey locusKey, List<MediaConnection> mediaConnectionList, final CallContext callContext) {
        // don't join if we're already in process of joining a locus
        if (!joiningLocus)  {
            joiningLocus = true;
            joiningLocusKey = locusKey;

            updateYearUtcTimezoneOffset(); // do this once per call in case user changed default timezone

            JoinLocusRequest joinLocusRequest = new JoinLocusRequest();
            joinLocusRequest.setDeviceUrl(deviceRegistration.getUrl());
            joinLocusRequest.setLocalMedias(mediaConnectionList);
            joinLocusRequest.setMoveMediaToResource(callContext.isMoveMediaToResource());

            if (deviceRegistration.getFeatures().isNativeClientLobbyEnabled()) {
                // TODO This data will live in the CallContext object when it is dynamic
                joinLocusRequest.setSupportsNativeLobby(true);
                joinLocusRequest.setModerator(false);
            }

            if (!TextUtils.isEmpty(callContext.getUsingResource())) {
                joinLocusRequest.setUsingResource(callContext.getUsingResource());
            }


            String url = locusKey.getUrl().toString() + "/participant";
            apiClientProvider.getHypermediaLocusClient().joinLocus(url, joinLocusRequest).enqueue(new Callback<JoinLocusResponse>() {
                @Override
                public void onResponse(Call<JoinLocusResponse> call, Response<JoinLocusResponse> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [joinLocus]", locusKey, getTrackingId(response));
                        callSuccess(response.body().getLocus(), callContext);
                    } else {
                        joiningLocus = false;
                        cancelJoin = false;
                        callFlowTrace("Locus", "App", "failure() [joinLocus]", locusKey, getTrackingId(response));
                        handleLocusError(response, true);
                    }

                }

                @Override
                public void onFailure(Call<JoinLocusResponse> call, Throwable t) {
                    joiningLocus = false;
                    cancelJoin = false;
                    Ln.i(t, "LocusService, joinLocus, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });

            callFlowTrace("App", "Locus", "[joinLocus]", locusKey);
        } else {
            Ln.i("LocusService.joinLocus, currently joining call");
        }
    }

    /**
     * Join Locus without media (empty MediaConnection list)
     *
     * @param locusKey locus to join
     * @param callContext context of call to setup
     */
    public void joinLocus(LocusKey locusKey, CallContext callContext) {
        joinLocus(locusKey, new ArrayList<MediaConnection>(), callContext);
    }

    /**
     * Call  - used for 1:1 calls to squared users and UC endpoints
     * https://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Locus-Call-Endpoint
     */
    public void call(final String invitee, List<MediaConnection> mediaConnectionList, final CallContext callContext) {

        joiningLocus = true;

        CallLocusRequest callLocusRequest = new CallLocusRequest();
        LocusInvitee locusInvitee = new LocusInvitee();
        locusInvitee.setInvitee(invitee);
        callLocusRequest.setInvitee(locusInvitee);
        callLocusRequest.setDeviceUrl(deviceRegistration.getUrl());
        callLocusRequest.setLocalMedias(mediaConnectionList);

        if (!TextUtils.isEmpty(callContext.getUsingResource())) {
            callLocusRequest.setUsingResource(callContext.getUsingResource());
        }

        if (deviceRegistration.getFeatures().isNativeClientLobbyEnabled()) {
            // TODO This data will live in the CallContext object when it is dynamic
            callLocusRequest.setSupportsNativeLobby(true);
            callLocusRequest.setModerator(false);
        }

        apiClientProvider.getLocusClient().callLocus(callLocusRequest).enqueue(new Callback<JoinLocusResponse>() {
            @Override
            public void onResponse(Call<JoinLocusResponse> call, Response<JoinLocusResponse> response) {
                if (response.isSuccessful()) {
                    callFlowTrace("Locus", "App", "success() [callLocus]", response.body().getLocus().getKey(), getTrackingId(response));
                    callSuccess(response.body().getLocus(), callContext);
                } else {
                    callFlowTrace("Locus", "App", "failure() [callLocus]", null, getTrackingId(response));
                    joiningLocus = false;
                    handleLocusError(response, true, invitee);
                }

            }
            public void onFailure(Call<JoinLocusResponse> call, Throwable t) {
                Ln.i(t, "LocusService, call, onFailure");
                bus.post(new RetrofitErrorEvent(t));
            }
        });

      callFlowTraceI("App", "Locus", "[callLocus]", Sanitize.sanitize(invitee));
    }


    /**
     * call without media
     * @param invitee
     * @param callContext
     */
    public void call(String invitee, final CallContext callContext) {
        call(invitee, new ArrayList<MediaConnection>(), callContext);
    }

    private void callSuccess(Locus locus, CallContext callContext) {
        LocusKey responseLocusKey = locus.getKey();
        if (callContext.getLocusKey() != null && !responseLocusKey.equals(callContext.getLocusKey())) {
            // redirect took place so locus url has changed
            locusDataCache.removeLocusData(callContext.getLocusKey());
            if (locus.getConversationUrl() == null) {
                ln.e(new RuntimeException("Received locus redirect without conversation url"));
            } else {
                bus.post(new LocusUrlUpdatedEvent(Uri.parse(locus.getConversationUrl()), callContext.getLocusKey(), responseLocusKey));
            }
        }

        locusProcessor.processLocusUpdate(locus);
        LocusData call = locusDataCache.getLocusData(responseLocusKey);

        int numberActiveParticipants = locus.getFullState().getCount();
        if (!cancelJoin) {
            call.setLocusKey(responseLocusKey);
            call.setCallInitiationOrigin(callContext.getCallInitiationOrigin());
            call.setVideoDisabled(false);
            call.setIsToasting(false);
            call.setObservingResource();
            // Observing resource is used by setRemoteParticipantDetails
            call.setRemoteParticipantDetails();

            // check if we're in joined state
            LocusParticipant selfParticipant = locus.getSelf();
            if (selfParticipant != null && selfParticipant.getState().equals(LocusParticipant.State.JOINED) ||
                    locus.isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
                List<LocusParticipant> joinedParticipants = new ArrayList<LocusParticipant>();
                joinedParticipants.add(selfParticipant);
                bus.post(new ParticipantJoinedEvent(locus.getKey(), locus, joinedParticipants));
            }
        }

        joiningLocus = false;

        // if we're answering a call and number of participants is 1 then that indicates that
        // other user left call while we were in process of joining...so leave locus
        // also leave locus if we or other user cancelled while we were joining
        // N.B. this logic will need to be updated for group calls where we are using bridge model
        // (i.e. where only one person can be on the call)
        boolean isObserving = call.isObserving(deviceRegistration.getUrl());
        if (!isObserving) {  // check that this isn't paired call
            if (cancelJoin || (callContext.isAnsweringCall() && (numberActiveParticipants == 1))) {
                Ln.i("LocusService.joinLocus, leaving locus as user that initiated call has left, cancelJoin = %b, numberActiveParticipants = %d", cancelJoin, numberActiveParticipants);
                cancelJoin = false;
                bus.post(new AnsweredInactiveCallEvent(responseLocusKey));
            }
        }
    }

    public boolean isJoiningLocus() {
        return joiningLocus;
    }

    /**
     * Add User to Locus
     */
    public void addUsersToLocus(final LocusKey locusKey, final List<LocusInvitee> invitees) {
        final LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            Observable.just(invitees)
                    .subscribeOn(schedulerProvider.newThread())
                    .map(locusInvitees -> queryUserIdentityKey(locusInvitees))
                    .map(map -> createUpdateLocusRequest(map, call, invitees))
                    .subscribe(updateLocusRequest -> {

                        apiClientProvider.getHypermediaLocusClient().updateLocus(locusKey.getUrl().toString(), updateLocusRequest).enqueue(new Callback<UpdateLocusResponse>() {
                           @Override
                           public void onResponse(Call<UpdateLocusResponse> call, Response<UpdateLocusResponse> response) {
                                if (response.isSuccessful()) {
                                    callFlowTrace("Locus", "App", "success() [addUsersToLocus]", locusKey, getTrackingId(response));
                                    locusProcessor.processLocusUpdate(response.body().getLocus());
                                } else {
                                    callFlowTrace("Locus", "App", "failure() [addUsersToLocus]", locusKey, getTrackingId(response));
                                    handleLocusError(response, false);
                                }
                           }

                           @Override
                           public void onFailure(Call<UpdateLocusResponse> call, Throwable t) {
                               Ln.i(t, "LocusService, addUsersToLocus, onFailure");
                               bus.post(new RetrofitErrorEvent(t));
                           }
                       });
                        callFlowTrace("App", "Locus", "[addUsersToLocus]", locusKey);
                    });
        }
    }

    private Map<String, UserIdentityKey> queryUserIdentityKey(List<LocusInvitee> invitees) {
        Map<String, UserIdentityKey> usersMap = null;
        List<UserEmailRequest> userEmailRequests = new ArrayList<>();
        for (LocusInvitee invitee: invitees) {
            userEmailRequests.add(new UserEmailRequest(invitee.getInvitee()));
        }
        try {
            retrofit2.Response<Map<String, UserIdentityKey>> resp = apiClientProvider.getUserClient().getOrCreateUserID(apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader(), userEmailRequests).execute();
            if (resp.isSuccessful()) {
                usersMap = resp.body();
            }
        } catch (IOException e) {
           Ln.e(e, "Get UserIdentityKey failed");
        }
        return usersMap;
    }

    private UpdateLocusRequest createUpdateLocusRequest(Map<String, UserIdentityKey> userIdentityKeyMap, LocusData call, List<LocusInvitee> inviteeEmails) {
        UpdateLocusRequest updateLocusRequest;
        if (userIdentityKeyMap != null) {
            List<LocusInvitee> inviteeIds = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (UserIdentityKey userIdentityKey: userIdentityKeyMap.values()) {
                LocusInvitee inviteeId = new LocusInvitee();
                inviteeId.setInvitee(userIdentityKey.getId());
                inviteeIds.add(inviteeId);
                ids.add(userIdentityKey.getId());
            }
            // invitees will be list of userId
            // ensure it if we generate the kmsMessage with userIds, if not, will failed to add guest.
            updateLocusRequest = new UpdateLocusRequest(inviteeIds, true);
            Uri aclUrl = call.getLocus().getAclUrl();
            if (deviceRegistration.getFeatures().isWhiteBoardAddGuestAclEnabled() && aclUrl != null) {
                try {
                    retrofit2.Response<Acl> response = apiClientProvider.getAclClient().get(aclUrl.toString()).execute();
                    if (response.isSuccessful()) {
                        String kmsResourceUrl = response.body().getKmsResourceUrl().toString();
                        Ln.e("locus data kmsResourceUrl:" + kmsResourceUrl);
                        Uri uri = Uri.parse(kmsResourceUrl);
                        KmsResourceObject kmsResourceObject = new KmsResourceObject(uri);
                        String kmsMessage = conversationProcessorLazy.get().authorizeNewParticipantsUsingKmsMessagingApi(kmsResourceObject, ids);
                        updateLocusRequest.setKmsMessage(kmsMessage);
                    }
                } catch (IOException e) {
                    Ln.e(e, "Get kmsMessage for updateLocusRequest failed");
                }
            }
        } else {
            // invitees will be list of email
            updateLocusRequest = new UpdateLocusRequest(inviteeEmails, true);
        }
        return updateLocusRequest;
    }

    public void mergeLoci(final LocusKey firstLocus, LocusKey secondLocus) {
        final LocusData call = locusDataCache.getLocusData(firstLocus);
        if (call != null) {
            LocusParticipant selfParticipant = call.getLocus().getSelf();

            call.setInitiatedMerge(true);
            MergeLociRequest mergeLociRequest = new MergeLociRequest(deviceRegistration.getUrl(), secondLocus.getLocusId());

            String url = selfParticipant.getUrl() + "/merge";
            apiClientProvider.getHypermediaLocusClient().mergeLoci(url, mergeLociRequest).enqueue(new Callback<MergeLociResponse>() {
                @Override
                public void onResponse(Call<MergeLociResponse> mergeLociResponseCall, Response<MergeLociResponse> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [mergeLoci]", firstLocus, getTrackingId(response));
                        locusProcessor.processLocusUpdate(response.body().getLocus());
                    } else {
                        callFlowTrace("Locus", "App", "failure() [mergeLoci]", firstLocus, getTrackingId(response));
                        call.setInitiatedMerge(false);
                        handleLocusError(response, false);
                    }

                }

                @Override
                public void onFailure(Call<MergeLociResponse> call, Throwable t) {
                    Ln.i(t, "LocusService, mergeLoci, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });

            callFlowTrace("App", "Locus", "[mergeLoci]", firstLocus);
        }
    }


    private static final int HTTP_LOCKED = 423;


    private void handleLocusError(Response response, boolean join) {
        handleLocusError(response, join, null);
    }


    private void handleLocusError(Response response, boolean join, String invitee) {
        Ln.w(response.errorBody().toString());

        Object event = null;
        int errorCode = response.code();
        String errorMessage = response.message();
        switch (errorCode) {
            case HttpURLConnection.HTTP_CONFLICT:
                event = new ConflictErrorJoiningLocusEvent(errorMessage, errorCode);
                break;
            case HttpURLConnection.HTTP_UNAVAILABLE:
                event = new HighVolumeErrorJoiningLocusEvent(errorMessage, errorCode);
                break;
            case HttpURLConnection.HTTP_FORBIDDEN:
            case HTTP_LOCKED:
                ErrorDetail errorDetail = getErrorDetailFromError(response);
                if (errorDetail != null && errorDetail.getErrorCode() != null) {
                    ErrorDetail.CustomErrorCode customErrorCode = ErrorDetail.CustomErrorCode.fromErrorCode(errorDetail.getErrorCode());
                    if (customErrorCode != null) {
                        if (errorCode == HttpURLConnection.HTTP_FORBIDDEN) {
                            event = new LocusInviteesExceedMaxSizeEvent(join, customErrorCode);
                        } else if (errorCode == HTTP_LOCKED) {
                            event = new LocusMeetingLockedEvent(join, customErrorCode);
                        }
                    } else {
                        Ln.w("Got unknown error code. Error Code: %s Detail: %s", errorDetail.getErrorCode(), errorDetail.getMessage());
                    }
                }
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
                errorDetail = getErrorDetailFromError(response);
                if (errorDetail != null && errorDetail.getErrorCode() != null) {
                    ErrorDetail.CustomErrorCode customErrorCode = ErrorDetail.CustomErrorCode.fromErrorCode(errorDetail.getErrorCode());
                    switch (customErrorCode) {
                        case LocusMeetingNotFound:
                        case LocusInvalidWebExSite:
                            event = new InvalidLocusEvent(customErrorCode, invitee);
                            break;
                        default:
                            Ln.w("Got unknown error code. Error Code: %s Detail: %s", errorDetail.getErrorCode(), errorDetail.getMessage());

                    }
                }
                break;
        }

        if (event == null) {
            event = new ErrorJoiningLocusEvent(errorMessage, errorCode);
        }
        bus.post(event);
    }

    // TODO: is there a better place for this?
    private ErrorDetail getErrorDetailFromError(Response response) {
        ErrorDetail errorDetail = null;
        try {
            errorDetail = gson.fromJson(response.errorBody().string(), ErrorDetail.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return errorDetail;
    }

    /**
     * Leave Locus
     * https://apidocs.wbx2.com/locus/html/api-TG9jdXMgU2VydmljZSAtIFYxX0xlYXZlIExvY3Vz.html
     */
    public void leaveLocus(final LocusKey locusKey, String usingResource) {

        // if we're currently in process of joining locus then set flag to indicate that we
        // should defer leaving locus until join completes.
        if (joiningLocus && joiningLocusKey != null && joiningLocusKey.equals(locusKey)) {
            cancelJoin = true;
            Ln.i("LocusService.leaveLocus, join in progress so deferring leave");
            return;
        }

        final LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            final LocusParticipant self = call.getLocus().getSelf();
            if (self != null && !self.getState().equals(LocusParticipant.State.LEAVING)) {

                try {
                    self.setState(LocusParticipant.State.LEAVING);

                    LeaveLocusRequest leaveLocusRequest = new LeaveLocusRequest();
                    leaveLocusRequest.setDeviceUrl(deviceRegistration.getUrl());

                    if (!TextUtils.isEmpty(usingResource)) {
                        leaveLocusRequest.setUsingResource(usingResource);
                    }

                    String url = self.getUrl() + "/leave";
                    apiClientProvider.getHypermediaLocusClient().leaveLocus(url, leaveLocusRequest).enqueue(new Callback<LeaveLocusResponse>() {
                        @Override
                        public void onResponse(Call<LeaveLocusResponse> leaveLocusResponseCall, Response<LeaveLocusResponse> response) {
                            if (response.isSuccessful()) {
                                callFlowTrace("Locus", "App", "success() [leaveLocus]", locusKey, getTrackingId(response));

                                // The user or room leaving the call must reset the state
                                call.resetObservingResource();

                                // this should update state to LEFT
                                locusProcessor.processLocusUpdate(response.body().getLocus());
                                bus.post(new LocusLeftEvent(locusKey));
                            } else {
                                Ln.w(response.message());
                                callFlowTrace("Locus", "App", "failure() [leaveLocus]", locusKey, getTrackingId(response));
                                bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                            }
                        }

                        @Override
                        public void onFailure(Call<LeaveLocusResponse> call, Throwable t) {
                            Ln.i(t, "LocusService, leaveLocus, onFailure");
                            bus.post(new RetrofitErrorEvent(t));
                        }
                    });
                    callFlowTrace("App", "Locus", "[leaveLocus]", locusKey);
                } catch (Exception e) {
                    Ln.w(e);
                }
            }
        }
    }




    public void leaveLocusSync(final LocusKey locusKey, String usingResource) {

        // if we're currently in process of joining locus then set flag to indicate that we
        // should defer leaving locus until join completes.
        if (joiningLocus && joiningLocusKey != null && joiningLocusKey.equals(locusKey)) {
            cancelJoin = true;
            Ln.i("LocusService.leaveLocus, join in progress so deferring leave");
            return;
        }

        final LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            final LocusParticipant self = call.getLocus().getSelf();
            if (self != null && !self.getState().equals(LocusParticipant.State.LEAVING)) {

                try {
                    LeaveLocusRequest leaveLocusRequest = new LeaveLocusRequest();
                    leaveLocusRequest.setDeviceUrl(deviceRegistration.getUrl());
                    leaveLocusRequest.setUsingResource(usingResource);

                    callFlowTrace("App", "Locus", "[leaveLocus]", locusKey);

                    String url = self.getUrl() + "/leave";
                    Response<LeaveLocusResponse> response = apiClientProvider.getHypermediaLocusClient().leaveLocus(url, leaveLocusRequest).execute();
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [leaveLocus]", locusKey, getTrackingId(response));

                        // The user or room leaving the call must reset the state
                        call.resetObservingResource();

                        // this should update state to LEFT
                        locusProcessor.processLocusUpdate(response.body().getLocus());
                        bus.post(new LocusLeftEvent(locusKey));
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [leaveLocus]", locusKey, getTrackingId(response));
                        bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                    }
                } catch (IOException e) {
                    Ln.i(e, "LocusService, leaveLocus, onFailure");
                    bus.post(new RetrofitErrorEvent(e));
                }
            }
        }
    }

    /**
     * Request that given participant is expelled from locus
     *
     * @param locusKey
     * @param participant
     */
    public void expelParticipant(final LocusKey locusKey, LocusParticipant participant) {
        if (locusDataCache.exists(locusKey)) {

            LocusControlRequest locusControlRequest = new LocusControlRequest();
            LocusLink locusLink = locusControlRequest.getExpelLocusLink(participant);

            String url = locusLink.getHref().toString() + "/leave";
            apiClientProvider.getHypermediaLocusClient().expelParticipant(url, locusLink.getBody()).enqueue(new Callback<LeaveLocusResponse>() {
                @Override
                public void onResponse(Call<LeaveLocusResponse> call, Response<LeaveLocusResponse> response) {
                    if (response.isSuccessful()) {
                        locusProcessor.processLocusUpdate(response.body().getLocus());
                        callFlowTrace("Locus", "App", "success() [expelParticipant]", locusKey, getTrackingId(response));
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [expelParticipant]", locusKey, getTrackingId(response));
                        bus.post(new CallControlMeetingControlsExpelEvent(locusKey, false));
                    }
                }

                @Override
                public void onFailure(Call<LeaveLocusResponse> call, Throwable t) {
                    Ln.i(t, "LocusService, expelParticipant, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });
        }
    }

    public void leaveLocus(final LocusKey locusKey) {
        leaveLocus(locusKey, "");
    }

    public void shareScreen(final LocusKey locusKey) {
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            MediaShare share = call.getLocus().getShareContentMedia();
            Floor floor = new Floor(Floor.GRANTED);
            LocusParticipant selfParticipant = call.getLocus().getSelf();
            floor.setBeneficiary(selfParticipant);
            floor.setRequester(selfParticipant);
            FloorShareRequest grantedRequest = new FloorShareRequest(floor);

            apiClientProvider.getHypermediaLocusClient().updateFloor(share.getUrl().toString(), grantedRequest).enqueue(new Callback<MediaShare>() {
                @Override
                public void onResponse(Call<MediaShare> call, Response<MediaShare> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [shareScreen]", locusKey, getTrackingId(response));
                        bus.post(new FloorRequestAcceptedEvent(MediaShare.SHARE_CONTENT_TYPE));
                    } else {
                        callFlowTrace("Locus", "App", "failure() [shareScreen]", locusKey, getTrackingId(response));
                        bus.post(new FloorRequestDeniedEvent(response.message(), response.code(), MediaShare.SHARE_CONTENT_TYPE));
                    }
                }

                @Override
                public void onFailure(Call<MediaShare> call, Throwable t) {
                    Ln.i(t, "LocusService, updateFloor, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });
            callFlowTrace("App", "Locus", "[shareScreen]", locusKey);
        }
    }

    public void unshareScreen(final LocusKey locusKey) {
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            MediaShare share = call.getLocus().getShareContentMedia();
            Floor floor = new Floor(Floor.RELEASED);
            FloorShareRequest releaseRequest = new FloorShareRequest(floor);


            apiClientProvider.getHypermediaLocusClient().updateFloor(share.getUrl().toString(), releaseRequest).enqueue(new Callback<MediaShare>() {
                @Override
                public void onResponse(Call<MediaShare> call, Response<MediaShare> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [unshareScreen]", locusKey, getTrackingId(response));
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [unshareScreen]", locusKey, getTrackingId(response));
                        bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                    }
                }

                @Override
                public void onFailure(Call<MediaShare> call, Throwable t) {
                    Ln.i(t, "LocusService, updateFloor, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });
            callFlowTrace("App", "Locus", "[unshareScreen]", locusKey);
        }
    }

    public boolean isSharingWhiteboard() {
        return sharingWhiteboard;
    }

    public void setSharingWhiteboard(boolean sharingWhiteboard) {
        this.sharingWhiteboard = sharingWhiteboard;
    }

    public void shareWhiteboard(final LocusKey locusKey, final String whiteboardUrl) {
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            MediaShare share = call.getLocus().getWhiteboardMedia();

            Floor floor = new Floor(Floor.GRANTED);
            LocusParticipant selfParticipant = call.getLocus().getSelf();
            floor.setBeneficiary(selfParticipant);
            floor.setRequester(selfParticipant);
            FloorShareRequest grantedRequest = new FloorShareRequest(floor, whiteboardUrl);
            apiClientProvider.getHypermediaLocusClient().updateFloor(share.getUrl().toString(), grantedRequest).enqueue(new Callback<MediaShare>() {
                @Override
                public void onResponse(Call<MediaShare> call, Response<MediaShare> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [shareWhiteboard]", locusKey, getTrackingId(response));
                        bus.post(new FloorRequestAcceptedEvent(MediaShare.SHARE_WHITEBOARD_TYPE));
                        sharingWhiteboard = true;
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [shareWhiteboard]", locusKey, getTrackingId(response));
                        sharingWhiteboard = false;
                        bus.post(new FloorRequestDeniedEvent(response.message(), response.code(), MediaShare.SHARE_WHITEBOARD_TYPE));
                    }
                }

                @Override
                public void onFailure(Call<MediaShare> call, Throwable t) {
                    Ln.i(t, "LocusService, shareWhiteboard, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });
            callFlowTrace("App", "Locus", "[shareWhiteboard]", locusKey);
        }
    }

    public void unshareWhiteboard(final LocusKey locusKey) {
        LocusData call = locusDataCache.getLocusData(locusKey);
        if (call != null) {
            MediaShare share = call.getLocus().getWhiteboardMedia();

            Floor floor = new Floor(Floor.RELEASED);
            FloorShareRequest releaseRequest = new FloorShareRequest(floor);
            apiClientProvider.getHypermediaLocusClient().updateFloor(share.getUrl().toString(), releaseRequest).enqueue(new Callback<MediaShare>() {
                @Override
                public void onResponse(Call<MediaShare> call, Response<MediaShare> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [unshareWhiteboard]", locusKey, getTrackingId(response));
                        sharingWhiteboard = false;
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [unshareWhiteboard]", locusKey, getTrackingId(response));
                        bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                    }
                }

                @Override
                public void onFailure(Call<MediaShare> call, Throwable t) {
                    Ln.i(t, "LocusService, unshareWhiteboard, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });
            callFlowTrace("App", "Locus", "[unshareWhiteboard]", locusKey);
        }
    }

    /**
     * Decline Locus
     * https://apidocs.wbx2.com/locus/html/api-TG9jdXMgU2VydmljZSAtIFYxX0RlY2xpbmUgTG9jdXM=.html
     *
     * @param locusKey locus url
     */
    public void declineLocus(final LocusKey locusKey, DeclineReason reason) {
        DeclineLocusRequest request = new DeclineLocusRequest(deviceRegistration.getUrl(), reason);

        String url = locusKey.getUrl().toString() + "/participant/decline";
        apiClientProvider.getHypermediaLocusClient().declineLocus(url, request).enqueue(new Callback<LocusResponse>() {
            @Override
            public void onResponse(Call<LocusResponse> call, Response<LocusResponse> response) {
                if (response.isSuccessful()) {
                    callFlowTrace("Locus", "App", "success() [declineLocus]", locusKey, getTrackingId(response));
                    locusProcessor.processLocusUpdate(response.body());
                } else {
                    Ln.w(response.message());
                    callFlowTrace("Locus", "App", "failure() [declineLocus]", locusKey, getTrackingId(response));
                    bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                }
            }

            @Override
            public void onFailure(Call<LocusResponse> call, Throwable t) {
                Ln.i(t, "LocusService, declineLocus, onFailure");
                bus.post(new RetrofitErrorEvent(t));
            }
        });
        callFlowTrace("App", "Locus", "Declining locus endpoint", locusKey);
    }


    /**
     * Alert Locus
     * https://apidocs.wbx2.com/locus/html/api-TG9jdXMgU2VydmljZSAtIFYxX0FsZXJ0IExvY3Vz.html
     * <p/>
     * Acknowledge that we received the NEW LOCUS event.
     *
     * @param locusKey locus url
     */
    public void alertLocus(final LocusKey locusKey) {
        AlertLocusRequest request = new AlertLocusRequest();
        request.setDeviceUrl(deviceRegistration.getUrl());

        String url = locusKey.getUrl().toString() + "/participant/alert";
        apiClientProvider.getHypermediaLocusClient().alertLocus(url, request).enqueue(new Callback<LocusResponse>() {
            @Override
            public void onResponse(Call<LocusResponse> call, Response<LocusResponse> response) {
                if (response.isSuccessful()) {
                    callFlowTrace("Locus", "App", "success() [alertLocus]", locusKey, getTrackingId(response));
                    locusProcessor.processLocusUpdate(response.body());
                } else {
                    Ln.w(response.message());
                    callFlowTrace("Locus", "App", "failure() [alertLocus]", locusKey, getTrackingId(response));
                    bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                }
            }

            @Override
            public void onFailure(Call<LocusResponse> call, Throwable t) {
                Ln.i(t, "LocusService, alertLocus, onFailure");
                bus.post(new RetrofitErrorEvent(t));
            }
        });
        callFlowTrace("App", "Locus", "Alerting locus endpoint", locusKey);
    }

    /**
     * Modify Media
     * Uses: https://apidocs.wbx2.com/locus/html/api-TG9jdXMgU2VydmljZSAtIFYxX21vZGlmeU1lZGlh.html
     *
     * @param locusKey  locus url
     * @param mediaConnectionList updated media connection info
     */
    public void modifyMedia(final LocusKey locusKey, List<MediaConnection> mediaConnectionList) {
        ModifyMediaRequest request = new ModifyMediaRequest();
        request.setLocalMedias(mediaConnectionList);
        request.setDeviceUrl(deviceRegistration.getUrl());

        if (locusDataCache.exists(locusKey)) {
            final LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null) {
                LocusParticipant selfParticipant = locusData.getLocus().getSelf();

                String url = selfParticipant.getUrl() + "/media";
                apiClientProvider.getHypermediaLocusClient().modifyMedia(url, request).enqueue(new Callback<ModifyMediaResponse>() {
                    @Override
                    public void onResponse(Call<ModifyMediaResponse> modifyMediaResponseCall, Response<ModifyMediaResponse> response) {
                        if (response.isSuccessful()) {
                            callFlowTrace("Locus", "App", "success() [modifyMedia]", locusKey, getTrackingId(response));
                            locusProcessor.processLocusUpdate(response.body().getLocus());

                            final LocusData call = getLocusData(response.body().getLocus().getKey());
                            if (call != null) {
                                Ln.d("modifyMedia.onSuccess, media was modified post MediaUpdatedEvent, %s", MediaSessionUtils.toString(call.getMediaSession()));
                            }
                            bus.post(new MediaUpdatedEvent());
                        } else {
                            Ln.w(response.message());
                            callFlowTrace("Locus", "App", "failure() [modifyMedia]", locusKey, getTrackingId(response));
                            bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<ModifyMediaResponse> call, Throwable t) {
                        Ln.w(t, "LocusService, modifyMedia, onFailure");
                        bus.post(new RetrofitErrorEvent(t));

                        boolean currentMutedState = locusData.getMediaSession().isAudioMuted();
                        MediaDirection selfMedia = currentMutedState ? MediaDirection.RECVONLY
                                                                        : MediaDirection.SENDRECV;

                        // if audio mute / unmute failed
                        if ((currentMutedState != selfParticipant.isAudioMuted())
                            && (selfMedia != selfParticipant.getStatus().audioStatus)) {
                            CallControlMeetingControlsEvent.Actor actor = CallControlMeetingControlsEvent.Actor.SELF;
                            bus.post(new CallControlParticipantAudioMuteEvent(locusKey, actor, false, selfParticipant, currentMutedState));
                        }
                    }
                });
                callFlowTrace("App", "Locus", "[modifyMedia]", locusKey);
            }
        }
    }



    public void holdLocus(final LocusKey locusKey) {
        LocusHoldRequest request = new LocusHoldRequest(deviceRegistration.getUrl());

        if (locusDataCache.exists(locusKey)) {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null) {
                LocusParticipant selfParticipant = locusData.getLocus().getSelf();

                String url = selfParticipant.getUrl() + "/hold";
                apiClientProvider.getHypermediaLocusClient().holdLocus(url, request).enqueue(new Callback<LocusParticipantResponse>() {
                    @Override
                    public void onResponse(Call<LocusParticipantResponse> call, Response<LocusParticipantResponse> response) {
                        if (response.isSuccessful()) {
                            callFlowTrace("Locus", "App", "success() [holdLocus]", locusKey, getTrackingId(response));
                            locusProcessor.processLocusUpdate(response.body().getLocus());
                        } else {
                            Ln.w(response.message());
                            callFlowTrace("Locus", "App", "failure() [holdLocus]", locusKey, getTrackingId(response));
                            bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<LocusParticipantResponse> call, Throwable t) {
                        Ln.i(t, "LocusService, holdLocus, onFailure");
                        bus.post(new RetrofitErrorEvent(t));
                    }
                });
                callFlowTrace("App", "Locus", "[holdLocus]", locusKey);
            }
        }
    }



    public void resumeLocus(final LocusKey locusKey) {
        LocusResumeRequest request = new LocusResumeRequest(deviceRegistration.getUrl());

        if (locusDataCache.exists(locusKey)) {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null) {
                LocusParticipant selfParticipant = locusData.getLocus().getSelf();
                UUID participantId = selfParticipant.getId();

                String url = selfParticipant.getUrl() + "/resume";
                apiClientProvider.getHypermediaLocusClient().resumeLocus(url, request).enqueue(new Callback<LocusParticipantResponse>() {
                    @Override
                    public void onResponse(Call<LocusParticipantResponse> call, Response<LocusParticipantResponse> response) {
                        if (response.isSuccessful()) {
                            callFlowTrace("Locus", "App", "success() [resumeLocus]", locusKey, getTrackingId(response));
                            locusProcessor.processLocusUpdate(response.body().getLocus());
                        } else {
                            Ln.w(response.message());
                            callFlowTrace("Locus", "App", "failure() [resumeLocus]", locusKey, getTrackingId(response));
                            bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<LocusParticipantResponse> call, Throwable t) {
                        Ln.i(t, "LocusService, resumeLocus, onFailure");
                        bus.post(new RetrofitErrorEvent(t));
                    }
                });
                callFlowTrace("App", "Locus", "[resumeLocus]", locusKey);
            }
        }
    }

    public void keepAlive(final LocusKey locusKey) {

        if (locusDataCache.exists(locusKey)) {

            LocusData locusData = locusDataCache.getLocusData(locusKey);
            LocusParticipantDevice localDevice = getCurrentDevice(locusData);

            if (localDevice != null && localDevice.getKeepAliveUrl() != null) {

                apiClientProvider.getHypermediaLocusClient().keepAlive(localDevice.getKeepAliveUrl().toString()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            callFlowTrace("Locus", "App", "success() [keepAlive]", locusKey, getTrackingId(response));
                        } else {
                            Ln.w(response.message());
                            callFlowTrace("Locus", "App", "failure() [keepAlive]", locusKey, getTrackingId(response));
                            bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Ln.i(t, "LocusService, keepAlive, onFailure");
                        bus.post(new RetrofitErrorEvent(t));
                    }
                });
                callFlowTrace("App", "Locus", "[keepAlive]", locusKey);

            } else {
                Ln.w("Unable to find local device to call keepalive");
            }
        }
    }

    @Nullable
    public LocusParticipantDevice getCurrentDevice(LocusData locusData) {

        LocusSelfRepresentation self = locusData.getLocus().getSelf();
        List<LocusParticipantDevice> devices = self.getDevices();

        for (LocusParticipantDevice device : devices) {
            if (device.getUrl().equals(deviceRegistration.getUrl())) {
                return device;
            }
        }

        return null;
    }


    /**
     * Create Media
     *
     * @param locusKey  locus url
     * @param mediaConnection new media connection info
     */
    public void createMedia(final LocusKey locusKey, MediaConnection mediaConnection) {
        MediaCreationRequest request = new MediaCreationRequest();
        request.setLocalMedia(mediaConnection);
        request.setDeviceUrl(deviceRegistration.getUrl());

        if (locusDataCache.exists(locusKey)) {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null) {
                LocusParticipant selfParticipant = locusData.getLocus().getSelf();

                String url = selfParticipant.getUrl() + "/media";
                apiClientProvider.getHypermediaLocusClient().createMedia(url, request).enqueue(new Callback<MediaCreationResponse>() {
                    @Override
                    public void onResponse(Call<MediaCreationResponse> modifyMediaResponseCall, Response<MediaCreationResponse> response) {
                        if (response.isSuccessful()) {
                            callFlowTrace("Locus", "App", "success() [createMedia]", locusKey, getTrackingId(response));
                            locusProcessor.processLocusUpdate(response.body().getLocus());
                            bus.post(new MediaCreatedEvent());
                        } else {
                            Ln.w(response.message());
                            callFlowTrace("Locus", "App", "failure() [createMedia]", locusKey, getTrackingId(response));
                            bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<MediaCreationResponse> call, Throwable t) {
                        Ln.i(t, "LocusService, createMedia, onFailure");
                        bus.post(new RetrofitErrorEvent(t));
                    }
                });
                callFlowTrace("App", "Locus", "[createMedia]", locusKey);
            }
        }
    }


    /**
     * Delete Media
     *
     * @param locusKey  locus url
     * @param mediaConnection   contains media ID to delete
     */
    public void deleteMedia(final LocusKey locusKey, MediaConnection mediaConnection) {

        if (locusDataCache.exists(locusKey)) {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null && mediaConnection != null) {

                String url = mediaConnection.getMediaUrl().toString();
                apiClientProvider.getHypermediaLocusClient().deleteMedia(url).enqueue(new Callback<MediaDeletionResponse>() {
                    @Override
                    public void onResponse(Call<MediaDeletionResponse> modifyMediaResponseCall, Response<MediaDeletionResponse> response) {
                        if (response.isSuccessful()) {
                            callFlowTrace("Locus", "App", "success() [deleteMedia]", locusKey, getTrackingId(response));
                        } else {
                            Ln.w(response.message());
                            callFlowTrace("Locus", "App", "failure() [deleteMedia]", locusKey, getTrackingId(response));
                            bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<MediaDeletionResponse> call, Throwable t) {
                        Ln.i(t, "LocusService, deleteMedia, onFailure");
                        bus.post(new RetrofitErrorEvent(t));
                    }
                });
                callFlowTrace("App", "Locus", "[deleteMedia]", locusKey);
            }
        }
    }

    @Nullable
    public Locus createAcl(final LocusKey locusKey, final String kmsMessage) {

        if (!locusDataCache.exists(locusKey)) {
            Ln.w("Unable to find call to create ACL in");
            return null;
        }

        LocusData locusData = locusDataCache.getLocusData(locusKey);
        Locus locus = locusData.getLocus();
        List<LocusParticipantInfo> people = locus.getUsers();
        CreateAclRequest request = new CreateAclRequest(kmsMessage, people);

        Locus aclLocus = null;
        try {
            String url = locusKey.getUrl().toString() + "/acl";
            Response<CreateAclResponse> response = apiClientProvider.getHypermediaLocusClient().createAcl(url, request).execute();
            if (response.isSuccessful()) {
                aclLocus = response.body().getLocus();
                locusProcessor.processLocusUpdate(aclLocus);
            } else {
                Ln.w(response.message());
                callFlowTrace("Locus", "App", "failure() [createAcl]", locusKey, getTrackingId(response));
                bus.post(new RetrofitErrorEvent(response.message(), response.code()));
            }
            return aclLocus;
        } catch (Exception e) {
            Ln.e(e);
        }

        return null;
    }

    /**
     * Modify Participant Controls
     */
    public void modifyParticipantControls(final LocusKey locusKey, final LocusParticipant participant, final boolean muteState) {
        if (locusDataCache.exists(locusKey)) {
            LocusControlRequest locusControlRequest = new LocusControlRequest();
            LocusLink locusLink = muteState ? locusControlRequest.getMuteLocusLink(participant)
                                            : locusControlRequest.getUnmuteLocusLink(participant);

            String url = locusLink.getHref().toString() + "/controls";
            apiClientProvider.getHypermediaLocusClient().modifyParticipantControls(url, locusLink.getBody()).enqueue(new Callback<ModifyMediaResponse>() {
                @Override
                public void onResponse(Call<ModifyMediaResponse> call, Response<ModifyMediaResponse> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [modifyParticipantControls]", locusKey, getTrackingId(response));
                        locusProcessor.processLocusUpdate(response.body().getLocus());
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [modifyParticipantControls]", locusKey, getTrackingId(response));
                        bus.post(new RetrofitErrorEvent(response.message(), response.code()));

                        CallControlMeetingControlsEvent.Actor actor = CallControlMeetingControlsEvent.Actor.MODERATOR;
                        bus.post(new CallControlParticipantAudioMuteEvent(locusKey, actor, false, participant, muteState));
                    }
                }

                @Override
                public void onFailure(Call<ModifyMediaResponse> call, Throwable t) {
                    Ln.i(t, "LocusService, modifyParticipantControls, onFailure");
                    bus.post(new RetrofitErrorEvent(t));

                    CallControlMeetingControlsEvent.Actor actor = CallControlMeetingControlsEvent.Actor.MODERATOR;
                    bus.post(new CallControlParticipantAudioMuteEvent(locusKey, actor, false, participant, muteState));
                }
            });
            callFlowTrace("App", "Locus", "[modifyParticipantControls]", locusKey);
        }
    }

    /**
     * Modify Locus control - lock/unlock and record meetings
     */
    public void modifyLocusControls(final LocusKey locusKey, LocusLink locusLink) {
        if (locusDataCache.exists(locusKey)) {

            String url = locusLink.getHref().toString() + "/controls";
            apiClientProvider.getHypermediaLocusClient().modifyLocusControls(url, locusLink.getBody()).enqueue(new Callback<LocusResponse>() {
                @Override
                public void onResponse(Call<LocusResponse> call, Response<LocusResponse> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [modifyLocustControls]", locusKey, getTrackingId(response));
                        locusProcessor.processLocusUpdate(response.body());
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [modifyLocusControls]", locusKey, getTrackingId(response));
                        bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                    }
                }

                @Override
                public void onFailure(Call<LocusResponse> call, Throwable t) {
                    Ln.i(t, "LocusService, modifyLocusControls, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });
        }
    }

    public void modifyLocusControls(final LocusKey locusKey, final boolean meetingLockedState) {
        if (locusDataCache.exists(locusKey)) {

            LocusControlRequest locusControlRequest = new LocusControlRequest();
            Locus locus =  locusDataCache.getLocusData(locusKey).getLocus();

            LocusLink locusLink = (meetingLockedState)
                    ? locusControlRequest.getUnlockLocusLink(locus)
                    : locusControlRequest.getLockLocusLink(locus);

            String url = locusLink.getHref().toString() + "/controls";
            apiClientProvider.getHypermediaLocusClient().modifyLocusControls(url, locusLink.getBody()).enqueue(new Callback<LocusResponse>() {
                @Override
                public void onResponse(Call<LocusResponse> call, Response<LocusResponse> response) {
                    if (response.isSuccessful()) {
                        callFlowTrace("Locus", "App", "success() [modifyLocustControls]", locusKey, getTrackingId(response));
                        locusProcessor.processLocusUpdate(response.body());
                    } else {
                        Ln.w(response.message());
                        callFlowTrace("Locus", "App", "failure() [modifyLocusControls]", locusKey, getTrackingId(response));
                        bus.post(new RetrofitErrorEvent(response.message(), response.code()));
                        bus.post(new CallControlMeetingControlsLockEvent(locusKey, false, !meetingLockedState));
                    }
                }

                @Override
                public void onFailure(Call<LocusResponse> call, Throwable t) {
                    Ln.i(t, "LocusService, modifyLocusControls, onFailure");
                    bus.post(new RetrofitErrorEvent(t));
                }
            });
        }
    }


    public Observable<Locus> getLocusById(String locusId) {
        return apiClientProvider.getLocusClient().getLocusById(locusId)
                .subscribeOn(schedulerProvider.newThread());

    }

    /**
     * Synchronize with current list of loci for this user on locus server
     * https://apidocs.wbx2.com/locus/html/api-TG9jdXMgU2VydmljZSAtIFYxX0dldCBMb2N1cyBMaXN0IFJlc3BvbnNl.html
     */
    public void synchronizeLoci() {
        Ln.d("synchronizeLoci, joiningLocus = " + joiningLocus);
        if (!joiningLocus) {
            try {
                clearLocusFlags();
                apiClientProvider.getLocusClient().getLoci(deviceRegistration.getFeatures().isSparkPMREnabled()).enqueue(new Callback<GetLocusListResponse>() {
                    @Override
                    public void onResponse(Call<GetLocusListResponse> call, Response<GetLocusListResponse> response) {
                        if (response.isSuccessful()) {
                            callFlowTrace("Locus", "App", "success() [getLoci]", null, getTrackingId(response));
                            locusDataCache.removeStaleLoci(response.body());
                            synchronized (locusDataCache) {

                                for (Locus locus : response.body().getLoci()) {
                                    if (locus.getFullState().isActive()) {
                                        Ln.i("synchronizeLoci, locus " + locus.getKey() + " in active state");
                                        locusProcessor.processLocusUpdate(locus);
                                    }
                                }
                            }
                        } else {
                            callFlowTrace("Locus", "App", "failed() [getLoci]", null, getTrackingId(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<GetLocusListResponse> call, Throwable t) {
                        Ln.i(t, "LocusService, getLoci, onFailure");

                    }
                });
                callFlowTrace("App", "Locus", "[getLoci]", null);
            } catch (Exception ex) {
                Ln.e(false, "Error calling getLoci", ex.getMessage());
            }
        }
    }

    // Event Handling

    public void onEvent(LocusChangedEvent event) {
        callFlowTrace("Mercury", "App", "locus changed, event type = " + event.getEventType(), event.getLocusKey(), getEventId(event));

        // TODO  still handling self_changed here...can we move this to LocusProcessor (and generate ParticipantSelfChangedEvent from there...
        // review use of ParticipantSelfChangedEvent event e.g. what part of self change are we looking to detect?
        locusProcessor.processLocusUpdate(event.getLocus());
        if (event.getEventType().equals(MercuryEventType.LOCUS_SELF_CHANGED)) {
            bus.post(new ParticipantSelfChangedEvent(event.getLocusKey(), event.getLocus()));
        }
    }

    public void onEvent(LocusDeltaEvent event) {
        callFlowTrace("Mercury", "App", "locus delta, event type = " + event.getEventType(), event.getLocusKey(), getEventId(event));

        if (deviceRegistration.getFeatures().isDeltaEventEnabled()) {
            //TODO need to send ParticipantSelfChangedEvent in the method processLocusUpdate
            locusProcessor.processLocusUpdate(event.getLocus(), new LocusDeltaEvent());
        } else {
            Ln.i("Received locus delta event, but it does not support on Spark Android client yet.");
        }
    }

    public void onEvent(final NewLocusEvent event) { // event from GCM
        callFlowTrace("GCM", "App", "NewLocusEvent", event.getLocusKey());

        // get locus info for locus url we received as part of invite.
        apiClientProvider.getHypermediaLocusClient().getLocus(event.getLocusKey().getUrl().toString()).enqueue(new Callback<Locus>() {
            @Override
            public void onResponse(Call<Locus> call, Response<Locus> response) {
                if (response.isSuccessful()) {
                    locusProcessor.processLocusUpdate(response.body());
                } else {
                    String eventString = "Unable to join the call. (1005)";
                    Ln.d("LocusService->EventBus: post(ErrorJoiningLocusEvent) - %s", eventString);
                    bus.post(new ErrorJoiningLocusEvent(response.message(), response.code()));
                }
            }

            @Override
            public void onFailure(Call<Locus> call, Throwable t) {
                Ln.i(t, "LocusService, getLocus, onFailure");
            }
        });
    }

    public class GetOrCreateMeetingInfoTask extends SafeAsyncTask<LocusMeetingInfo> {
        private final Action<LocusMeetingInfo> successCallback;
        private final Action<Exception> failureCallback;
        private final LocusKey locusKey;

        public GetOrCreateMeetingInfoTask(@NonNull LocusKey locusKey, @NonNull Action<LocusMeetingInfo> successCallback, @Nullable Action<Exception> failureCallback) {
            super();
            this.locusKey = locusKey;
            this.successCallback = successCallback;
            this.failureCallback = failureCallback;
        }

        @Override
        public LocusMeetingInfo call() throws Exception {
            return apiClientProvider.getLocusClient().getOrCreateMeetingInfo(locusKey.getLocusId()).execute().body();
        }

        @Override
        protected void onSuccess(LocusMeetingInfo locusMeetingInfo) throws Exception {
            successCallback.call(locusMeetingInfo);
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            super.onException(e);
            if (failureCallback != null) {
                failureCallback.call(e);
            }
        }
    }


    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }

    public Response<Void> sendDtmf(final LocusKey locusKey, int correlationId, String tones) {
        SendDtmfRequest request = new SendDtmfRequest();
        request.setDtmf(correlationId, tones);
        request.setDeviceUrl(deviceRegistration.getUrl());

        Response<Void> response = null;

        if (locusDataCache.exists(locusKey)) {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null) {
                try {
                    LocusParticipant selfParticipant = locusData.getLocus().getSelf();

                    String url = selfParticipant.getUrl() + "/sendDtmf";
                    response = apiClientProvider.getHypermediaLocusClient().sendDtmf(url, request).execute();
                    callFlowTrace("App", "Locus", "[sendDtmf]", locusKey);
                    if (!response.isSuccessful()) {
                        Ln.e(false, "sendDtmf", response.message());
                    }
                } catch (Exception ex) {
                    Ln.e(false, ex, "Error send DTMF tone", ex.getMessage());
                }
            }
        }

        return response;
    }

    public Response migrateLocus(final LocusKey locusKey, String inviteeEmail, String targetEmail, boolean isMigrateSelf) {
        MigrateRequest migrateRequest = new MigrateRequest(inviteeEmail, isMigrateSelf, deviceRegistration.getUrl());

        Response response = null;

        if (locusDataCache.exists(locusKey)) {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locusData != null) {
                try {
                    LocusParticipant targetParticipant = locusData.getParticipantByEmail(targetEmail);

                    String url = targetParticipant.getUrl().toString() + "/migrate";
                    response = apiClientProvider.getHypermediaLocusClient().migrateLocus(url, migrateRequest).execute();
                    if (!response.isSuccessful()) {
                        Ln.e(false, "migrateLocus", response.message());
                    }
                } catch (Exception ex) {
                    Ln.e(false, "Error migrating locus", ex.getMessage());
                }
            }
        }

        return response;
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        updateYearUtcTimezoneOffset(); // do this on startup
        Ln.i("App->LocusService: start()");

        // Call on start, so UI gets updated
        synchronizeLoci();
    }

    @Override
    public void stop() {
        Ln.i("App->LocusService: stop()");
    }

    public void post(Object object) {
        bus.post(object);
    }

    // LocusDataCache interface methods

    public LocusData getLocusData(LocusKey locusKey) {
        return locusDataCache.getLocusData(locusKey);
    }

    public boolean exists(LocusKey locusKey) {
        return locusDataCache.exists(locusKey);
    }

    private String updateYearUtcTimezoneOffset() {
        Date date = new Date();
        String year = String.format(Locale.US, "%2$tY", "", date);
        int offsetMin = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000 * 60);
        boolean bNegativeOffset = false;
        if (offsetMin < 0) {
            bNegativeOffset = true;
            offsetMin = -offsetMin;
        }
        int offsetHr = offsetMin / 60;
        offsetMin -= offsetHr * 60;

        yearUtcTimezoneOffset = String.format(Locale.US, "%s, %s%02d:%02d", year, bNegativeOffset ? "-" : "+", offsetHr, offsetMin);
        return yearUtcTimezoneOffset;
    }

    private String getYearUtcTimezoneOffset() {
        return yearUtcTimezoneOffset;
    }


    public void callFlowTrace(final String from, final String to, final String trace, final LocusKey locusKey) {
        callFlowTrace(from, to, trace, locusKey, trackingIdGenerator.currentTrackingId());
    }

    private void callFlowTrace(final String from, final String to, final String trace, final LocusKey locusKey, final String uniqueId) {
        Lns.callFlow().i.message(from, to, String.format(Locale.US, "%s [%s][%s][%s]", trace, locusKey == null ? "<NULL>" : locusKey.getLocusId(), uniqueId, getYearUtcTimezoneOffset()));
    }

    private void callFlowTraceI(final String from, final String to, final String trace, final String invitee) {
        Lns.callFlow().i.message(from, to, String.format(Locale.US, "%s [%s][%s][%s]", trace, invitee == null ? "<NULL>" : invitee, getYearUtcTimezoneOffset(), trackingIdGenerator.currentTrackingId()));
    }

    private String getTrackingId(Response response) {
        String trackingId = null;
        if (response != null) {
            trackingId = response.headers().get("trackingid");
        }
        if (trackingId == null) {
            Ln.w(false, "Response header contained no TrackingId.  Using the 'currentId + *' as approximation.");
            trackingId = trackingIdGenerator.currentTrackingId() + "*";
        }
        return trackingId;
    }

    private String getEventId(LocusEvent event) {
        return ((event != null && event.getId() != null) ? event.getId().toString() : "");
    }


    // TODO can we move this up to CallControlService
    private void clearLocusFlags() {
        ArrayList<ContentProviderOperation> operation = new ArrayList<>();
        operation.add(clearConversationInActiveCall());
        try {
            context.getContentResolver().applyBatch(ConversationContract.CONTENT_AUTHORITY, operation);
        } catch (Exception ex) {
            Ln.e(ex, "Failed to clear locus flags in database");
        }
    }
}
