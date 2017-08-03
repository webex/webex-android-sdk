package com.cisco.spark.android.whiteboard;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.acl.Acl;
import com.cisco.spark.android.acl.AclLinkRequest;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.client.AclClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.events.KmsKeyEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheChangedEvent;
import com.cisco.spark.android.locus.events.WhiteboardShareErrorEvent;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipantInfo;
import com.cisco.spark.android.locus.model.MediaShare;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.github.benoitdion.ln.Ln;
import com.jakewharton.rxrelay.PublishRelay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.schedulers.Schedulers;

import static com.cisco.spark.android.whiteboard.WhiteboardShareDelegate.ShareRequest.NOT_SHARING;
import static com.cisco.spark.android.whiteboard.WhiteboardShareDelegate.ShareRequest.REQUESTED;
import static com.cisco.spark.android.whiteboard.WhiteboardShareDelegate.ShareRequest.REQUESTING;
import static com.cisco.spark.android.whiteboard.WhiteboardShareDelegate.ShareRequest.SHARING;
import static com.cisco.spark.android.whiteboard.WhiteboardShareDelegate.ShareRequest.STOP_REQUESTING;
import static com.cisco.spark.android.whiteboard.util.WhiteboardUtils.getIdFromUrl;

public class WhiteboardShareDelegate implements Component {

    public enum ShareRequest {
        REQUESTED,
        CREATE_WAIT,
        REQUESTING,
        NOT_SHARING,
        STOP_REQUESTING,
        SHARING
    }

    public enum ShareRequestErrorState {

        // Fatals
        BAD_CHANNEL(true),
        BAD_LOCUS(true),
        NO_KEYS(true),
        NO_USERS(true),
        GET_ACL(true),

        // Non-fatals
        CREATE_ACL(false),
        LINK_ACL(false),
        FLOOR_NOT_GRANTED(false),

        UNKNOWN(false);

        boolean fatal;

        ShareRequestErrorState(boolean fatal) {
            this.fatal = fatal;
        }

        public boolean isFatal() {
            return fatal;
        }
    }

    private final EncryptedConversationProcessor conversationProcessor;
    private final CallControlService callControlService;
    private final DeviceRegistration deviceRegistration;
    private final ApiClientProvider apiClientProvider;
    private final WhiteboardService whiteboardService;
    private final LocusDataCache locusDataCache;
    private final LocusService locusService;
    private final KeyManager keyManager;
    private final EventBus eventBus;

    private String sharedBoardId;

    private Channel shareRequestChannel;
    private LocusKey currentLocus;
    private String keyRequest;

    private ShareRequest shareState;

    private PublishRelay<ShareState> shareStateStream;

    private Object shareStateLock = new Object();

    /**
     * The dependencies/coupling here need to be fixed, but we need to do this in small steps so we don't break anything.
     */
    public WhiteboardShareDelegate(EncryptedConversationProcessor conversationProcessor, LocusDataCache locusDataCache,
                                   CallControlService callControlService, DeviceRegistration deviceRegistration,
                                   ApiClientProvider apiClientProvider, WhiteboardService whiteboardService,
                                   LocusService locusService, KeyManager keyManager, EventBus eventBus) {

        this.conversationProcessor = conversationProcessor;

        this.locusDataCache = locusDataCache;
        this.callControlService = callControlService;
        this.deviceRegistration = deviceRegistration;
        this.apiClientProvider = apiClientProvider;
        this.whiteboardService = whiteboardService;
        this.locusService = locusService;
        this.keyManager = keyManager;
        this.eventBus = eventBus;

        shareStateStream = PublishRelay.create();
        setShareState(NOT_SHARING);
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
        }
        setShareState(NOT_SHARING);
    }

    @Override
    public void stop() {
        if (eventBus.isRegistered(this)) {
            eventBus.unregister(this);
        }
        // TODO Need to tidy up any outgoing requests
    }

    public ShareRequest getShareRequestState() {
        return shareState;
    }

    public Observable<ShareState> getShareRequestStream() {
        return shareStateStream;
    }

    public void stopShare() {
        setShareState(STOP_REQUESTING);
        Observable.just(this).subscribeOn(Schedulers.computation())
                .subscribe(shareDelegate -> {

                    Channel channel = shareRequestChannel;
                    if (channel != null && channel.isPrivateChannel()) {
                        unlinkAcl(channel, locusDataCache.getActiveLocus());
                    }

                    callControlService.unshareWhiteboard();

                }, Ln::e);
    }

    public boolean adHocShare(final Channel channel, final LocusKey locusKey) {

        // It's fine to do if we've already made a request
        if (shareState == ShareRequest.REQUESTING) {
            // TODO Make a queue of requests
            return false;
        }
        setShareState(ShareRequest.REQUESTING);

        Observable.just(this).subscribeOn(Schedulers.computation())
                  .subscribe(shareDelegate -> privateShare(channel, locusKey), Ln::e);

        return true;
    }

    private synchronized void privateShare(Channel channel, LocusKey locusKey) {

        Ln.d("Trying to share whiteboard into call");
        shareRequestChannel = channel;
        currentLocus = locusKey;

        Locus locus = extractValidLocus(locusKey);
        if (locus == null) {
            shareFailed(ShareRequestErrorState.BAD_LOCUS, "No valid locus to share to");
            return;
        }

        Ln.d("Trying to share whiteboard into ad hoc call");
        if (locus.getAclUrl() != null) {
            linkAcl(locus);
            return;
        }

        Locus locusWithAcl = createAcl(channel, locus, locusKey);
        if (locusWithAcl != null) {
            linkAcl(locusWithAcl);
        } else {
            if (keyRequest == null) {
                shareFailed(ShareRequestErrorState.CREATE_ACL, "Unable to add ACL to locus");
            }
        }
    }

    @Nullable
    private Locus createAcl(Channel channel, Locus locus, LocusKey locusKey) {

        KeyObject key = keyManager.getCachedUnboundKey();
        if (key == null) {
            keyRequest = channel.getChannelUrl();
            Ln.w("No unbound keys, retrying later");
            return null;
        } else {
            keyRequest = null;
        }

        List<String> userIds = new ArrayList<>();
        List<LocusParticipantInfo> ciUsers = locus.getCiUsers(true);

        for (LocusParticipantInfo ciUser : ciUsers) {
            userIds.add(ciUser.getId());
        }

        if (userIds.size() == 1) {
            shareFailed(ShareRequestErrorState.NO_USERS, "Couldn't find any valid users to add to KMS");
            return null;
        }

        String locusKmsMessage = conversationProcessor.createNewResource(userIds,
                                                                         Collections.singletonList(key.getKeyId()));
        return locusService.createAcl(locusKey, locusKmsMessage);
    }

    private void unlinkAcl(Channel channel, LocusKey locusKey) {

        if (channel == null || locusKey == null) {
            stopShareFailed("Unable to unlink acl due to nullness (c)" + channel + "   (l) " + locusKey);
            return;
        }

        if (channel.getAclUrl() == null) {
            stopShareFailed("No valid ACL URL in channel to unlink");
            return;
        }

        Locus locus = extractValidLocus(locusKey);
        if (locus == null) {
            stopShareFailed("No valid locus to unlink ACL from");
            return;
        }

        Acl locusAcl = getAcl(locus.getAclUrl());
        if (locusAcl == null) {
            stopShareFailed("Unable to link ACL because failed to get locus ACL");
            return;
        }

        KmsResourceObject boardKro = new KmsResourceObject(channel.getKmsResourceUrl());
        String aclKmsMessage = conversationProcessor.removeParticipantUsingKmsMessagingApi(boardKro, locusAcl.getKmsResourceUrl().toString());

        final Call<Response<ResponseBody>> aclLinkResponse = apiClientProvider.getAclClient(Uri.parse(channel.getAclUrl()))
                .linkAcl(new AclLinkRequest(AclLinkRequest.AclLinkOperation.DELETE,
                        locus.getAclUrl(),
                        aclKmsMessage));

        Observable.just(aclLinkResponse).subscribeOn(Schedulers.newThread()).subscribe(responseCall -> {

            Response response;
            try {
                response = aclLinkResponse.execute();
            } catch (IOException e) {
                stopShareFailed(e.getMessage());
                Ln.e(e);
                return;
            }

            if (response.isSuccessful()) {
                setShareState(ShareRequest.NOT_SHARING);
                callControlService.unshareWhiteboard();
            } else {
                stopShareFailed("Failed to unlink ACLs");
            }
        }, Ln::e);
    }


    private void linkAcl(Locus locus) {

        String aclUrl = shareRequestChannel.getAclUrl();
        if (aclUrl == null) {
            shareFailed(ShareRequestErrorState.BAD_CHANNEL, "Channel doesn't have an ACL");
            return;
        }

        // We need the ACL for the KMS resource URL
        Acl locusAcl = getAcl(locus.getAclUrl());
        if (locusAcl == null) {
            shareFailed(ShareRequestErrorState.GET_ACL, "Unable to link ACL because failed to get locus ACL");
            return;
        }

        KmsResourceObject boardKro = new KmsResourceObject(shareRequestChannel.getKmsResourceUrl());

        String aclKmsMessage = conversationProcessor.authorizeNewParticipantsUsingKmsMessagingApi(boardKro, Collections.singletonList(locusAcl.getKmsResourceUrl().toString()));
        Call<Response<ResponseBody>> aclLinkResponse = apiClientProvider.getAclClient(Uri.parse(aclUrl))
                                                                        .linkAcl(new AclLinkRequest(
                                                                                AclLinkRequest.AclLinkOperation.ADD,
                                                                                locus.getAclUrl(),
                                                                                aclKmsMessage));
        Response response;
        try {
            response = aclLinkResponse.execute();
        } catch (IOException e) {
            shareFailed(ShareRequestErrorState.LINK_ACL, e.getMessage());
            Ln.e(e);
            return;
        }

        if (response.isSuccessful()) {
            setShareState(ShareRequest.REQUESTED);
            callControlService.shareWhiteboard(shareRequestChannel.getChannelUrl());
        } else {
            shareFailed(ShareRequestErrorState.LINK_ACL, "Failed to link ACLs");
        }
    }

    public synchronized void share(Channel channel, String conversationId) {

        if (!locusDataCache.isInCall() || conversationId == null || channel == null) {
            return;
        }

        if (shareState == ShareRequest.REQUESTING) {
            // TODO MAke a queue of requests
            return;
        }

        shareRequestChannel = channel;
        setShareState(ShareRequest.REQUESTING);

        Ln.d("Trying to share whiteboard into call");
        LocusKey locusKey = locusDataCache.getActiveLocus();
        LocusData locusData = locusDataCache.getLocusData(locusKey);

        if (locusData.getLocus() == null) {
            shareFailed(ShareRequestErrorState.BAD_LOCUS,
                        "Trying to share whiteboard into a null call");
            return;
        }

        if (locusData.getLocus().getConversationUrl() == null) {
            shareFailed(ShareRequestErrorState.BAD_LOCUS,
                        "Trying to share whiteboard into a call without a conversation. This isn't currently supported");
            return;
        }

        if (!callControlService.isConversationInCall(conversationId)) {
            shareFailed(ShareRequestErrorState.BAD_LOCUS,
                        "Trying to share a whiteboard into a call for wrong conversation");
            return;
        }

        setShareState(ShareRequest.REQUESTED);

        if (!TextUtils.isEmpty(channel.getChannelUrl())) {
            callControlService.shareWhiteboard(channel.getChannelUrl());
        } else {
            Ln.e("Shared whiteboard failed because no valid channelUrl");
        }
    }

    @Nullable
    private Locus extractValidLocus(LocusKey locusKey) {

        if (locusKey == null) {
            Ln.w("A locus key is required for private share");
            return null;
        }

        LocusData locusData = locusDataCache.getLocusData(locusKey);
        if (locusData == null) {
            Ln.w("Unable to share, couldn't find locus data for %s", locusKey);
            return null;
        }

        Locus locus = locusData.getLocus();
        if (locus == null) {
            Ln.w("Unable to share, couldn't find locus for %s", locusKey);
            return null;
        }

        if (locus.getConversationUrl() != null) {
            Ln.w("Unable to share into call with no conversation");
            return null;
        }

        return locus;
    }

    @Nullable
    private Acl getAcl(Uri aclUrl) {

        deviceRegistration.whitelist(aclUrl);
        AclClient aclClient = apiClientProvider.getAclClient(aclUrl);

        try {
            Response<Acl> response = aclClient.get(aclUrl.toString()).execute();

            if (response.isSuccessful()) {
                return response.body();
            } else {
                Ln.e("Get ACL returned " + response.code());
                return null;
            }

        } catch (IOException e) {
            Ln.e(e);
            return null;
        }
    }

    public String getSharedBoardId() {
        return sharedBoardId;
    }

    public void onEventAsync(WhiteboardShareErrorEvent event) {
        shareFailed(ShareRequestErrorState.FLOOR_NOT_GRANTED, event.getErrorMessage());
    }

    public void onEventAsync(SelfRemovedFromAclEvent event) {

        if (!event.isSuccess()) {
            return;
        }

        if (event.getChannelId().equals(sharedBoardId)) {
            Ln.i("Stopping share due to deleted board %s", event.getChannelId());
            stopShare();
        }
    }

    public synchronized void onEventAsync(LocusDataCacheChangedEvent event) {
        if (!whiteboardService.isWhiteboardEnabled()) {
            return;
        }

        LocusKey locusKey = event.getLocusKey();
        LocusData data;
        Locus locus;
        MediaShare whiteboardMedia;

        if (locusKey != null && (data = locusDataCache.getLocusData(locusKey)) != null &&
            (locus = data.getLocus()) != null && (whiteboardMedia = locus.getWhiteboardMedia()) != null) {

            if (whiteboardMedia.isMediaShareGranted()) {
                String eventBoardId = getIdFromUrl(whiteboardMedia.getResourceUrl());
                if (sharedBoardId == null
                        || !sharedBoardId.equals(eventBoardId)
                        ||  shareRequestChannel != null && shareRequestChannel.getChannelId().equals(eventBoardId)) {

                    sharedBoardId = eventBoardId;
                    setShareState(SHARING);
                    return;
                }
            } else if (shareState == SHARING) {
                sharedBoardId = null;
                setShareState(NOT_SHARING);
                return;
            }
        }

        if (shareState == STOP_REQUESTING) {
            setShareState(NOT_SHARING);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public synchronized void onEventAsync(KmsKeyEvent event) {
        // We have tried to share a private board, failed, and successfully got new keys
        if (shareRequestChannel != null && currentLocus != null) {
            if (keyRequest != null) {
                privateShare(shareRequestChannel, currentLocus);
            }
        }
    }

    private synchronized void setShareState(ShareRequest newState, @Nullable ShareRequestErrorState errorState,
                                            @Nullable String message) {
        synchronized (shareStateLock) {
            if (newState == NOT_SHARING) {
                sharedBoardId = null;
            }

            shareState = newState;
            shareStateStream.call(new ShareState(newState, errorState, message));
        }
    }

    private synchronized void setShareState(ShareRequest newState) {
        setShareState(newState, null, "");
    }

    private void shareFailed(ShareRequestErrorState errorState, String message) {
        sharedBoardId = null;
        setShareState(NOT_SHARING, errorState, message);
        Ln.e(message);
    }

    private void stopShareFailed(String message) {
        setShareState(SHARING);
        Ln.e(message);
    }

    public boolean isRequesting(String boardId) {
        return ongoingRequest() && shareRequestChannel != null && boardId != null &&
               boardId.equals(shareRequestChannel.getChannelId());
    }

    private boolean ongoingRequest() {
        synchronized (shareStateLock) {
            return shareState == REQUESTING || shareState == REQUESTED || shareState == STOP_REQUESTING;
        }
    }

    public static class ShareState {

        @NonNull private ShareRequest state;
        @Nullable private ShareRequestErrorState error;
        @Nullable private String message;

        public ShareState(@NonNull ShareRequest state, @Nullable ShareRequestErrorState error,
                          @Nullable String message) {
            this.state = state;
            this.error = error;
            this.message = message;
        }

        @NonNull
        public ShareRequest getState() {
            return state;
        }

        @Nullable
        public ShareRequestErrorState getError() {
            return error;
        }

        @Nullable
        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return error == null;
        }
    }
}
