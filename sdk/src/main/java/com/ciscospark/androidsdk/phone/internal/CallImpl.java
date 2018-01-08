/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.phone.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.cisco.spark.android.callcontrol.events.CallControlMediaDecodeSizeChangedEvent;
import com.cisco.spark.android.events.OperationCompletedEvent;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.model.MediaDirection;
import com.cisco.spark.android.media.MediaRequestSource;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.sync.operationqueue.SendDtmfOperation;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.internal.ResultImpl;
import com.ciscospark.androidsdk.phone.Call;
import com.ciscospark.androidsdk.phone.CallMembership;
import com.ciscospark.androidsdk.phone.CallObserver;
import com.ciscospark.androidsdk.phone.MediaOption;
import com.ciscospark.androidsdk.phone.Phone;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class CallImpl implements Call {

    private @NonNull PhoneImpl _phone;

    @StringPart
    private @NonNull CallStatus _status;

    @StringPart
    private @NonNull Direction _direction;

    @StringPart
    private @NonNull LocusKey _key;

    private MediaOption _option;

    private CallObserver _observer;

    private CompletionHandler<Void> _answerCallback;

    private CompletionHandler<Void> _rejectCallback;

    private CompletionHandler<Void> _hangupCallback;

    private Rect _localVideoViewSize = new Rect(0, 0, 0, 0);
    private Rect _remoteVideoViewSize = new Rect(0, 0, 0, 0);

    private Map<SendDtmfOperation, CompletionHandler<Void>> _dtmfOperations = new HashMap<>(1);

    private boolean _isGroup;

    CallImpl(@NonNull PhoneImpl phone, @Nullable MediaOption option, @NonNull Direction direction, @NonNull LocusKey key,boolean group) {
        _phone = phone;
        _option = option;
        _direction = direction;
        _key = key;
        _status = CallStatus.INITIATED;
        _isGroup = group;
    }

    @NonNull
    LocusKey getKey() {
        return _key;
    }

    void setStatus(@NonNull CallStatus status) {
        _status = status;
    }

    MediaOption getOption() {
        return _option;
    }

    void setMediaOption(MediaOption option) {
        _option = option;
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
            memberships.add(new CallMembershipImpl(p));
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

    public boolean isRemoteSendingVideo() {
        for (LocusParticipant p : getRemoteParticipants()) {
            if (p.getState() == LocusParticipant.State.JOINED
                && p.getStatus().getVideoStatus().equals(MediaDirection.SENDONLY) || p.getStatus().getVideoStatus().equals(MediaDirection.SENDRECV)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRemoteSendingAudio() {
        for (LocusParticipant p : getRemoteParticipants()) {
            if (p.getState() == LocusParticipant.State.JOINED
                && p.getStatus().getAudioStatus().equals(MediaDirection.SENDONLY) || p.getStatus().getAudioStatus().equals(MediaDirection.SENDRECV)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSendingVideo() {
        return _option != null && _option.hasVideo() && !_phone.getCallService().isVideoMuted(getKey());
    }

    public void setSendingVideo(boolean sending) {
        if (_option == null || !_option.hasVideo()){
            Ln.d("Can not setSendingVideo in a Audio call, return");
            return;
        }
        if (sending) {
            _phone.getCallService().unMuteVideo(getKey(), MediaRequestSource.USER);
        }
        else {
            _phone.getCallService().muteVideo(getKey(), MediaRequestSource.USER);
        }
    }

    public boolean isSendingAudio() {
        return !_phone.getCallService().isAudioMuted(getKey());
    }

    public void setSendingAudio(boolean sending) {
        if (sending) {
            _phone.getCallService().unmuteAudio(getKey());
        }
        else {
            _phone.getCallService().muteAudio(getKey());
        }
    }

    public boolean isReceivingVideo() {
        return _option != null && _option.hasVideo() && !_phone.getCallService().isRemoteVideoMuted(getKey());
    }

    public void setReceivingVideo(boolean receiving) {
        if (_option == null || !_option.hasVideo()){
            Ln.d("Can not setReceivingVideo in a Audio call, return");
            return;
        }
        _phone.getCallService().muteRemoteVideos(getKey(), !receiving);
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

    public void acknowledge(@NonNull CompletionHandler<Void> callback) {
        _phone.getCallService().acknowledge(getKey());
        setStatus(CallStatus.RINGING);
        // TODO post event when locus alert responsed in common lib
        callback.onComplete(null);
    }

    public void answer(@NonNull MediaOption option, @NonNull CompletionHandler<Void> callback) {
        _option = option;
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
        SendDtmfOperation operation = _phone.getOperationQueue().sendDtmf(dtmf);
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
            if (session != null) {
                if (session.setSelectedCameraAndChangeDevice(PhoneImpl.fromFacingMode(facingMode))) {
                    CallObserver observer = getObserver();
                    if (observer != null) {
                        observer.onMediaChanged(new CallObserver.CameraSwitched(this));
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlMediaDecodeSizeChangedEvent event) {
        Ln.d("CallControlMediaDecodeSizeChangedEvent is received");
        _remoteVideoViewSize = event.getSize();
        CallObserver observer = getObserver();
        if (observer != null) {
            observer.onMediaChanged(new CallObserver.RemoteVideoViewSizeChanged(this));
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
                }
                else {
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
        LocusData locusData = _phone.getCallService().getLocusData(getKey());
        if (locusData == null) {
            return new ArrayList<>(0);
        }
        Locus locus = locusData.getLocus();
        if (locus == null) {
            return new ArrayList<>(0);
        }
        return locus.getParticipants();
    }

    private List<LocusParticipant> getRemoteParticipants() {
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

    boolean isGroup() { return _isGroup; }
}