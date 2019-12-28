/*
 * Copyright 2016-2019 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.phone.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Rect;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.events.CallControlMediaDecodeSizeChangedEvent;
import com.cisco.spark.android.events.OperationCompletedEvent;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.model.MediaDirection;
import com.cisco.spark.android.locus.model.MediaShare;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaRequestSource;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.sync.operationqueue.SendDtmfOperation;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.phone.AuxStream;
import com.ciscowebex.androidsdk.phone.Call;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.CallObserver;
import com.ciscowebex.androidsdk.phone.MediaOption;
import com.ciscowebex.androidsdk.phone.MultiStreamObserver;
import com.ciscowebex.androidsdk.phone.Phone;
import com.ciscowebex.androidsdk.utils.Utils;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;
import com.google.gson.JsonArray;

import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class CallImpl implements Call {

    private @NonNull
    PhoneImpl _phone;

    @StringPart
    private @NonNull
    CallStatus _status;

    @StringPart
    private @NonNull
    Direction _direction;

    @StringPart
    private @NonNull
    LocusKey _key;

    private MediaOption _option;

    private CallObserver _observer;

    private CompletionHandler<Void> _answerCallback;

    private CompletionHandler<Void> _rejectCallback;

    private CompletionHandler<Void> _hangupCallback;

    private CompletionHandler<Void> _shareRequestCallback;

    private CompletionHandler<Void> _shareReleaseCallback;

    private Rect _localVideoViewSize = new Rect(0, 0, 0, 0);

    private Rect _remoteVideoViewSize = new Rect(0, 0, 0, 0);

    private Rect _sharingViewSize = new Rect(0, 0, 0, 0);

    private Map<SendDtmfOperation, CompletionHandler<Void>> _dtmfOperations = new HashMap<>(1);

    private boolean _isGroup;

    private View _sharingRenderView;

    private Pair<View, View> _videoRenderViews;

    private List<AuxStreamImpl> _openedAuxStreamList = new ArrayList<>();

    private int _availableAuxStreamCount;

    private CallMembership _activeSpeaker;

    private MultiStreamObserver _multiStreamObserver;

    CallImpl(@NonNull PhoneImpl phone, @Nullable MediaOption option, @NonNull Direction direction, @NonNull LocusKey key, boolean group) {
        _phone = phone;
        _direction = direction;
        _key = key;
        _status = CallStatus.INITIATED;
        _isGroup = group;
        setMediaOption(option);
    }

    @NonNull
    LocusKey getKey() {
        return _key;
    }

    void setStatus(@NonNull CallStatus status) {
        Ln.d("CallStatus from " + _status + " to " + status + ", " + this);
        _status = status;
    }

    MediaOption getOption() {
        return _option;
    }

    void setMediaOption(@Nullable MediaOption option) {
        _option = option;
        if (option != null) {
            if (option.getLocalView() != null && option.getRemoteView() != null) {
                _videoRenderViews = new Pair<>(option.getLocalView(), option.getRemoteView());
            } else {
                _videoRenderViews = null;
            }
            _sharingRenderView = option.getSharingView();
        }
    }

    CompletionHandler<Void> getAnswerCallback() {
        return _answerCallback;
    }

    CompletionHandler<Void> getRejectCallback() {
        return _rejectCallback;
    }

    CompletionHandler<Void> getHangupCallback() {
        return _hangupCallback;
    }

    CompletionHandler<Void> getShareRequestCallback() {
        return _shareRequestCallback;
    }

    CompletionHandler<Void> getShareReleaseCallback() {
        return _shareReleaseCallback;
    }

    @NonNull
    public CallStatus getStatus() {
        return _status;
    }

    @NonNull
    public Call.Direction getDirection() {
        return _direction;
    }

    public void setObserver(CallObserver observer) {
        _observer = observer;
    }

    public CallObserver getObserver() {
        return _observer;
    }

    public List<CallMembership> getMemberships() {
        List<LocusParticipant> participants = getParticipants();
        List<CallMembership> memberships = new ArrayList<>(participants.size());
        for (LocusParticipant p : participants) {
            memberships.add(new CallMembershipImpl(p, this));
        }
        return memberships;
    }

    public List<CallMembership> getJoinedMemberships() {
        List<LocusParticipant> participants = getParticipants();
        List<CallMembership> memberships = new ArrayList<>(participants.size());
        for (LocusParticipant p : participants) {
            if (p.getState() == LocusParticipant.State.JOINED)
                memberships.add(new CallMembershipImpl(p, this));
        }
        return memberships;
    }

    public CallMembership getFrom() {
        for (CallMembership membership : getMemberships()) {
            if (membership.isInitiator()) {
                return membership;
            }
        }
        return null;
    }

    public CallMembership getTo() {
        for (CallMembership membership : getMemberships()) {
            if (!membership.isInitiator()) {
                return membership;
            }
        }
        return null;
    }

    public Rect getLocalVideoViewSize() {
        return _localVideoViewSize;
    }

    public Rect getRemoteVideoViewSize() {
        return _remoteVideoViewSize;
    }

    public Rect getSharingViewSize() {
        return _sharingViewSize;
    }

    public boolean isRemoteSendingVideo() {
        return _phone.getRemoteSendingVideo();
    }

    public boolean isRemoteSendingAudio() {
        for (LocusParticipant p : getRemoteParticipants()) {
            if (p.getState() == LocusParticipant.State.JOINED && p.getStatus() != null) {
                MediaDirection direction = p.getStatus().getAudioStatus();
                if (direction == null || direction == MediaDirection.SENDONLY || direction == MediaDirection.SENDRECV) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isRemoteSendingSharing() {
        LocusData locus = _phone.getCallService().getLocusData(getKey());
        return locus != null && locus.isFloorGranted() && !_phone.isSharingFromThisDevice(locus);
    }

    public boolean isSendingSharing() {
        LocusData locus = _phone.getCallService().getLocusData(getKey());
        return locus != null && _phone.isSharingFromThisDevice(locus);
    }

    @Override
    public int getOpenedAuxStreamCount() {
        return _openedAuxStreamList.size();
    }

    public void setAvailableAuxStreamCount(int count) {
        _availableAuxStreamCount = count;
    }

    @Override
    public int getAvailableAuxStreamCount() {
        return _availableAuxStreamCount;
    }

    public void setActiveSpeaker(CallMembership person) {
        _activeSpeaker = person;
    }

    @Override
    public CallMembership getActiveSpeaker() {
        return _activeSpeaker;
    }

    @Override
    public void setMultiStreamObserver(MultiStreamObserver observer) {
        _multiStreamObserver = observer;
    }

    @Override
    public MultiStreamObserver getMultiStreamObserver() {
        return _multiStreamObserver;
    }

    public AuxStreamImpl getAuxStream(long vid) {
        for (AuxStreamImpl auxStream : _openedAuxStreamList) {
            if (auxStream.getVid() == vid)
                return auxStream;
        }
        return null;
    }

    @Override
    public AuxStream getAuxStream(View view) {
        for (AuxStreamImpl auxStream : _openedAuxStreamList) {
            if (auxStream.getRenderView() == view)
                return auxStream;
        }
        return null;
    }

    @Override
    public void openAuxStream(@NonNull View view) {
        Ln.d("openAuxStream: " + view);
        String error = null;
        if (getOpenedAuxStreamCount() >= _availableAuxStreamCount || getOpenedAuxStreamCount() >= MediaEngine.MAX_NUMBER_STREAMS) {
            error = "Reach maximum count";
        }

        if (getAuxStream(view) != null) {
            error = "This view has been used";
        }

        if (!isGroup()) {
            error = "Only can be used for group call";
        }

        if (error == null) {
            long vid = _phone.getCallService().subscribeRemoteAuxVideo(getKey(), view);
            Ln.d("openAuxStream vid: " + vid);
            if (vid >= 0) {
                AuxStreamImpl auxStream = new AuxStreamImpl(getKey(), _phone, vid, view);
                _openedAuxStreamList.add(auxStream);
                if (view instanceof SurfaceView) {
                    ((SurfaceView) view).getHolder().addCallback(new SurfaceHolder.Callback() {
                        @Override
                        public void surfaceCreated(SurfaceHolder surfaceHolder) {
                            Ln.d("remote surfaceCreated vid: " + vid);
                            if (!_phone.getCallService().isRemoteWindowAttached(_key, vid, view))
                                _phone.getCallService().setRemoteWindowForVid(_key, vid, view);
                        }

                        @Override
                        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                            Ln.d("remote surfaceChanged vid: " + vid);
                        }

                        @Override
                        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                            Ln.d("remote surfaceDestroyed vid: " + vid);
                            _phone.getHandler().post(() -> _phone.getCallService().removeRemoteWindowForVid(_key, vid, view));
                        }
                    });
                }
            } else {
                error = "Open aux stream error";
            }
        }

        if (_multiStreamObserver != null) {
            _multiStreamObserver.onAuxStreamChanged(new MultiStreamObserver.AuxStreamOpenedEvent(this, view, error));
        }
    }

    @Override
    public void closeAuxStream(@NonNull View view) {
        Ln.d("closeAuxStream: " + view);
        closeAuxStream(getAuxStream(view), view);
    }

    public void closeAuxStream(AuxStream auxStream, View view) {
        Ln.d("closeAuxStream auxStream: " + auxStream);
        String error = null;
        if (auxStream != null && _phone.getCallService().unsubscribeRemoteAuxVideo(getKey(), ((AuxStreamImpl) auxStream).getVid())) {
            _openedAuxStreamList.remove(auxStream);
        } else {
            error = "Close aux stream error";
        }

        if (_multiStreamObserver != null) {
            _multiStreamObserver.onAuxStreamChanged(new MultiStreamObserver.AuxStreamClosedEvent(this, view, error));
        }
    }

    public void closeAuxStream() {
        if (getOpenedAuxStreamCount() > _availableAuxStreamCount) {
            AuxStream auxStream = _openedAuxStreamList.get(_openedAuxStreamList.size() - 1);
            closeAuxStream(auxStream, auxStream.getRenderView());
        }
    }

    public boolean isSendingVideo() {
        return _option != null && _option.hasVideo() && !_phone.getCallService().isVideoMuted(getKey());
    }

    public void setSendingVideo(boolean sending) {
        if (_option == null || !_option.hasVideo()) {
            Ln.d("Can not setSendingVideo in a Audio call, return");
            return;
        }
        if (sending) {
            _phone.getCallService().unMuteVideo(getKey(), MediaRequestSource.USER);
        } else {
            _phone.getCallService().muteVideo(getKey(), MediaRequestSource.USER);
        }
    }

    public boolean isSendingAudio() {
        return !_phone.getCallService().isAudioMuted(getKey());
    }

    public void setSendingAudio(boolean sending) {
        if (sending) {
            _phone.getCallService().unmuteAudio(getKey());
        } else {
            _phone.getCallService().muteAudio(getKey());
        }
    }

    public boolean isReceivingVideo() {
        return _option != null && _option.hasVideo() && !_phone.getCallService().isRemoteVideoMuted(getKey());
    }

    public void setReceivingVideo(boolean receiving) {
        if (_option == null || !_option.hasVideo()) {
            Ln.d("Can not setReceivingVideo in a Audio call, return");
            return;
        }
        _phone.getCallService().muteRemoteVideo(getKey(), !receiving);
        CallObserver observer = getObserver();
        if (observer != null) {
            observer.onMediaChanged(new CallObserver.ReceivingVideo(this, receiving));
        }
    }

    public boolean isReceivingAudio() {
        return !_phone.getCallService().isRemoteAudioMuted(getKey());
    }

    public void setReceivingAudio(boolean receiving) {
        _phone.getCallService().muteRemoteAudio(getKey(), !receiving);
        CallObserver observer = getObserver();
        if (observer != null) {
            observer.onMediaChanged(new CallObserver.ReceivingAudio(this, receiving));
        }
    }

    @Override
    public boolean isReceivingSharing() {
        if (!_option.hasSharing() && _option.hasVideo()) {
            return isReceivingVideo();
        } else if (_option.hasSharing()) {
            return !_phone.getCallService().isRemoteScreenShareMuted(getKey());
        } else {
            return false;
        }
    }

    @Override
    public void setReceivingSharing(boolean receiving) {
        if (_option == null || (!_option.hasSharing() && !_option.hasVideo())) {
            Ln.d("Can not setReceivingSharing in a Audio call, return");
            return;
        }

        if (!_option.hasSharing() && _option.hasVideo()) {
            if (!isRemoteSendingSharing()) {
                // not have sharing and have video and the remote doesn't start sharing.
                // do nothing when user set the receivingSharing
                return;
            }
            setReceivingVideo(receiving);
        } else {
            _phone.getCallService().muteRemoteScreenShare(getKey(), !receiving);
        }

        CallObserver observer = getObserver();
        if (observer != null) {
            observer.onMediaChanged(new CallObserver.ReceivingSharing(this, receiving));
        }
    }

    public Pair<View, View> getVideoRenderViews() {
        return _videoRenderViews;
    }

    @Override
    public void setVideoRenderViews(@Nullable Pair<View, View> videoRenderViews) {
        if (_status == CallStatus.DISCONNECTED) {
            return;
        }
        Ln.d("setVideoRenderViews, old: " + _videoRenderViews + ", new: " + videoRenderViews);
        if (videoRenderViews != null && (videoRenderViews.first == null || videoRenderViews.second == null)) {
            Ln.e("The local and remote video views must be set in same time");
            return;
        }
        if (_videoRenderViews != null && (_videoRenderViews.first == null || _videoRenderViews.second == null)) {
            Ln.e("The local and remote video views must be set in same time");
            return;
        }
        CallControlService service = _phone.getCallService();
        LocusKey key = getKey();
        if (_videoRenderViews == null && videoRenderViews == null) {
            Ln.d("Do nothing.");
        }
        else if (_videoRenderViews == null) {
            _videoRenderViews = videoRenderViews;
            service.setPreviewWindow(key, _videoRenderViews.first);
            service.setRemoteWindow(key, _videoRenderViews.second);
            updateMedia();
        }
        else if (videoRenderViews == null) {
            _videoRenderViews = null;
            service.removePreviewWindows(key);
            service.removeRemoteVideoWindows(key);
            updateMedia();
        }
        else {
            if (_videoRenderViews.first != videoRenderViews.first) {
                service.removePreviewWindow(key, _videoRenderViews.first);
                service.setPreviewWindow(key, videoRenderViews.first);
            }
            if (_videoRenderViews.second != videoRenderViews.second) {
                service.removeRemoteWindow(key, _videoRenderViews.second);
                service.setRemoteWindow(key, videoRenderViews.second);
            }
            _videoRenderViews = videoRenderViews;
        }
    }

    public View getSharingRenderView() {
        return _sharingRenderView;
    }

    @Override
    public void setSharingRenderView(View view) {
        if (_status == CallStatus.DISCONNECTED) {
            return;
        }
        if (_sharingRenderView == null && view == null) {
            return;
        }
        CallControlService service = _phone.getCallService();
        LocusKey key = getKey();
        View oldView = _sharingRenderView;
        _sharingRenderView = view;
        if (oldView != null && view == null) {
            service.removeShareWindow(key);
            updateMedia();
            return;
        }
        if (oldView == null && view != null) {
            service.setShareWindow(key, view);
            updateMedia();
            return;
        }
        if (oldView != view) {
            service.removeShareWindow(key);
            service.setShareWindow(key, view);
        }
    }

    public void acknowledge(@NonNull CompletionHandler<Void> callback) {
        _phone.getCallService().acknowledge(getKey());
        // TODO post event when locus alert responsed in common lib
        callback.onComplete(null);
    }

    public void answer(@NonNull MediaOption option, @NonNull CompletionHandler<Void> callback) {
        setMediaOption(option);
        _answerCallback = callback;
        _phone.answer(this);
    }

    public void reject(@NonNull CompletionHandler<Void> callback) {
        _rejectCallback = callback;
        _phone.reject(this);
    }

    public void hangup(@NonNull CompletionHandler<Void> callback) {
        _hangupCallback = callback;
        _phone.hangup(this);
    }

    public boolean isSendingDTMFEnabled() {
        LocusSelfRepresentation self = getSelf();
        return self != null && self.isEnableDTMF();
    }

    public void sendDTMF(@NonNull String dtmf, @NonNull CompletionHandler<Void> callback) {
        SendDtmfOperation operation = _phone.getOperations().sendDtmf(dtmf);
        _dtmfOperations.put(operation, callback);
    }

    public void sendFeedback(int rating, @Nullable String comment) {
        Map<String, String> info = new HashMap<>();
        info.put("user.rating", String.valueOf(rating));
        info.put("user.comments", comment);
        info.put("locusId", this._key.getLocusId());
        Locus locus = _phone.getCallService().getLocus(_key);
        if (locus != null && locus.getSelf() != null) {
            info.put("participantId", locus.getSelf().getId().toString());
        }
        _phone.sendFeedback(info);
    }

    @Override
    public void startSharing(@NonNull CompletionHandler<Void> callback) {
        if (_status == CallStatus.CONNECTED) {
            LocusData locusData = _phone.getCallService().getLocusData(_key);
            if (locusData != null) {
                MediaShare contentMediaShare = locusData.getLocus().getShareContentMedia();
                if (contentMediaShare == null || !contentMediaShare.isMediaShareGranted() || !_phone.isSharingFromThisDevice(locusData)) {
                    _shareRequestCallback = callback;
                    _phone.startSharing(_key);
                } else if (callback != null) {
                    Ln.w("Can not startSharing, because call is sharing content");
                    callback.onComplete(ResultImpl.error("Call is sharing content"));
                }
            } else if (callback != null) {
                Ln.w("startSharing callControlService.getLocusData is null");
                callback.onComplete(ResultImpl.error("Call is not exist"));
            }
        } else if (callback != null) {
            Ln.w("Can not startSharing, because call status is: " + _status);
            callback.onComplete(ResultImpl.error("Call is not connected"));
        }
    }

    @Override
    public void stopSharing(@NonNull CompletionHandler<Void> callback) {
        if (_status == CallStatus.CONNECTED) {
            LocusData locusData = _phone.getCallService().getLocusData(_key);
            if (locusData != null) {
                MediaShare contentMediaShare = locusData.getLocus().getShareContentMedia();
                if (contentMediaShare != null && contentMediaShare.isMediaShareGranted() && _phone.isSharingFromThisDevice(locusData)) {
                    _shareReleaseCallback = callback;
                    _phone.stopSharing(_key);
                } else if (callback != null) {
                    Ln.w("Can not stopSharing, because call is not sharing content");
                    callback.onComplete(ResultImpl.error("Call is not sharing content"));
                }
            } else if (callback != null) {
                Ln.w("stopSharing callControlService.getLocusData is null");
                callback.onComplete(ResultImpl.error("Call is not exist"));
            }
        } else if (callback != null) {
            Ln.w("Can not stopSharing, because call status is: " + _status);
            callback.onComplete(ResultImpl.error("Call is not connected"));
        }
    }

    public Phone.FacingMode getFacingMode() {
        com.cisco.spark.android.callcontrol.model.Call call = _phone.getCallService().getCall(getKey());
        if (call == null) {
            return _phone.getDefaultFacingMode();
        }
        MediaSession session = call.getMediaSession();
        if (session == null) {
            return _phone.getDefaultFacingMode();
        }
        return PhoneImpl.toFacingMode(session.getSelectedCamera());
    }

    public void setFacingMode(Phone.FacingMode facingMode) {
        com.cisco.spark.android.callcontrol.model.Call call = _phone.getCallService().getCall(getKey());
        if (call != null) {
            MediaSession session = call.getMediaSession();
            if (session != null && !session.getSelectedCamera().equals(PhoneImpl.fromFacingMode(facingMode))) {
                session.switchCamera();
                CallObserver observer = getObserver();
                if (observer != null) {
                    observer.onMediaChanged(new CallObserver.CameraSwitched(this));
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlMediaDecodeSizeChangedEvent event) {
        Ln.d("CallControlMediaDecodeSizeChangedEvent is received");
        if (event.getVideoId() != 0)
            return;
        CallObserver.MediaChangedEvent mediaEvent = null;
        if (event.getVideoId() == MediaEngine.SHARE_MID) {
            _sharingViewSize = event.getSize();
            mediaEvent = new CallObserver.RemoteSharingViewSizeChanged(this);
        } else {
            _remoteVideoViewSize = event.getSize();
            mediaEvent = new CallObserver.RemoteVideoViewSizeChanged(this);
        }
        CallObserver observer = getObserver();
        if (observer != null) {
            observer.onMediaChanged(mediaEvent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MediaSession.MediaRenderSizeChangedEvent event) {
        Ln.d("MediaRenderSizeChangedEvent is received");
        _localVideoViewSize = event.size;
        CallObserver observer = getObserver();
        if (observer != null) {
            observer.onMediaChanged(new CallObserver.LocalVideoViewSizeChanged(this));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OperationCompletedEvent event) {
        Ln.d("OperationCompletedEvent is received");
        Operation operation = event.getOperation();
        if (operation != null) {
            CompletionHandler<Void> callback = _dtmfOperations.get(operation);
            if (callback != null) {
                if (operation.isSucceeded()) {
                    callback.onComplete(ResultImpl.success(null));
                } else {
                    callback.onComplete(ResultImpl.error(operation.getErrorMessage()));
                }
            }
        }
    }

    @Override
    public String toString() {
        return Objects.toStringByAnnotation(this);
    }

    LocusSelfRepresentation getSelf() {
        LocusData locusData = _phone.getCallService().getLocusData(getKey());
        if (locusData == null) {
            return null;
        }
        Locus locus = locusData.getLocus();
        if (locus == null) {
            return null;
        }
        return locus.getSelf();
    }

    private List<LocusParticipant> getParticipants() {
        List<LocusParticipant> participants = new ArrayList<>();
        LocusData locusData = _phone.getCallService().getLocusData(getKey());
        if (locusData == null) {
            return participants;
        }
        Locus locus = locusData.getLocus();
        if (locus == null) {
            return participants;
        }
        for (LocusParticipant p : locus.getParticipants()) {
            if (p.getType() == LocusParticipant.Type.USER || p.getType() == LocusParticipant.Type.RESOURCE_ROOM) {
                participants.add(p);
            }
        }
        return participants;
    }

    protected List<LocusParticipant> getRemoteParticipants() {
        List<LocusParticipant> participants = getParticipants();
        LocusSelfRepresentation self = getSelf();
        if (self == null) {
            return participants;
        }
        List<LocusParticipant> ret = new ArrayList<>(participants.size() - 1);
        for (LocusParticipant p : participants) {
            if (!self.getId().equals(p.getId())) {
                ret.add(p);
            }
        }
        return ret;
    }

    boolean isGroup() {
        return _isGroup;
    }

    LocusParticipant getSharingSender() {
        LocusData locus = _phone.getCallService().getLocusData(getKey());
        if (locus != null && locus.isFloorGranted()) {
            return _phone.getCallService().getLocusData(getKey()).getParticipantSharing();
        }
        return null;
    }

    void updateMedia() {
        com.cisco.spark.android.callcontrol.model.Call locus = _phone.getCallService().getCall(getKey());
        if (locus != null) {
            _phone.getCallService().updateMediaSession(locus, PhoneImpl.mediaOptionToMediaDirection(_option));
        }
    }

    @Override
    // TODO Callback
    public void letIn(@NonNull CallMembership membership) {
        String personId = WebexId.translate(membership.getPersonId());
        List<LocusParticipant> participants = getRemoteParticipants();
        for (LocusParticipant participant : participants) {
            if (participant.isInLobby() && personId.equals(participant.getPerson().getId())) {
                _phone.getCallService().admitParticipant(participant);
                return;
            }
        }
    }

    @Override
    // TODO Callback
    public void letIn(@NonNull List<CallMembership> memberships) {
        JsonArray jsonArray = new JsonArray();
        List<LocusParticipant> participants = getRemoteParticipants();
        for (LocusParticipant participant : participants) {
            if (participant.isInLobby()) {
                for (CallMembership membership : memberships) {
                    String personId = WebexId.translate(membership.getPersonId());
                    if (personId.equals(participant.getPerson().getId())) {
                        jsonArray.add(participant.getId().toString());
                    }
                }
            }
        }
        if (jsonArray.size() != 0) {
            _phone.getCallService().admitAllParticipant(jsonArray);
        }
    }
}
