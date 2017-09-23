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
import java.util.UUID;
import javax.inject.Inject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.events.CallControlCallCancelledEvent;
import com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLeaveLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalAudioMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusCreatedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantAudioMuteEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantJoinedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlSelfParticipantLeftEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.events.CallNotificationEvent;
import com.cisco.spark.android.events.CallNotificationType;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.events.RequestCallingPermissions;
import com.cisco.spark.android.locus.events.LocusDeclinedEvent;
import com.cisco.spark.android.locus.events.ParticipantNotifiedEvent;
import com.cisco.spark.android.locus.events.RetrofitErrorEvent;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.Spark;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.core.SparkInjector;
import com.ciscospark.androidsdk.phone.Call;
import com.ciscospark.androidsdk.phone.CallObserver;
import com.ciscospark.androidsdk.phone.CallOption;
import com.ciscospark.androidsdk.phone.Phone;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
import de.greenrobot.event.EventBus;


/**
 * Created by zhiyuliu on 04/09/2017.
 */
public class PhoneImpl implements Phone {

    private static final String TAG = "PhoneImpl";

    @Inject
    ApplicationController _applicationController;

    @Inject
    ApiClientProvider apiClientProvider;

    @Inject
    CallControlService _callControlService;

    @Inject
    OperationQueue _operationQueue;

    @Inject
    MediaEngine _mediaEngine;

    @Inject
    EventBus _bus;

    private IncomingCallListener _incomingCallListener;

    private Authenticator _authenticator;

    private DeviceRegistration _device;

    private Handler _registerTimer;

    private Runnable _registerTimeoutTask;

    private CompletionHandler<Void> _registerCallback;

    private Map<LocusKey, CallImpl> _calls = new HashMap<>();

    private CompletionHandler<Call> _dialCallback;

    private CompletionHandler<Void> _incomingCallback;

    private CallOption _option;

    private MediaSession _preview;
    
    private H264LicensePrompter _prompter;

    public PhoneImpl(Context context, Authenticator authenticator, SparkInjector injector) {
        injector.inject(this);
        _authenticator = authenticator;
        _bus.register(this);
        _registerTimer = new Handler();
        _prompter = new H264LicensePrompter(context.getSharedPreferences(Spark.class.getPackage().getName(), Context.MODE_PRIVATE));
    }

    public IncomingCallListener getIncomingCallListener() {
        return _incomingCallListener;
    }

    public void setIncomingCallListener(IncomingCallListener listener) {
        _incomingCallListener = listener;
    }

    public void close() {
        if (_bus.isRegistered(this)) {
            _bus.unregister(this);
        }
    }

    public FacingMode getDefaultFacingMode() {
        return toFacingMode(_callControlService.getDefaultCamera());
    }

    public void setDefaultFacingMode(FacingMode mode) {
        _callControlService.setDefaultCamera(fromFacingMode(mode));
    }
    
    public void requestVideoCodecActivation(@NonNull AlertDialog.Builder builder, @NonNull CompletionHandler<Boolean> callback) {
        _prompter.check(builder, result -> {
            callback.onComplete(Result.success(result));
        });
    }
    
    public void disableVideoCodecActivation() {
        _prompter.setVideoLicenseActivationDisabled(true);
    }

    public void register(@NonNull CompletionHandler<Void> callback) {
        Log.i(TAG, "register: ->start");
        if (_registerCallback != null) {
            Log.w(TAG, "Registering");
            callback.onComplete(Result.error("Registering"));
            return;
        }
        _registerCallback = callback;

        ServiceBuilder.async(_authenticator, callback, s -> {
            _registerTimeoutTask = () -> {
                Log.i(TAG, "run: -> register timeout");
                if (_device == null && _registerCallback != null) {
                    _registerCallback.onComplete(Result.error("Register timeout"));
                }
            };
            _registerTimer.postDelayed(_registerTimeoutTask, 60 * 1000);
            new AuthenticatedUserTask(_applicationController).execute();
        });
    }

    public void deregister(@NonNull CompletionHandler<Void> callback) {
        Log.i(TAG, "deregister: ->start");
        _applicationController.logout(null, false, false, false);
        _mediaEngine.uninitialize();
        _device = null;
        _registerCallback = null;
        _registerTimer.removeCallbacks(_registerTimeoutTask);
        callback.onComplete(Result.success(null));
    }

    public void startPreview(View view) {
        stopPreview();
        _preview = _mediaEngine.createMediaSession(UUID.randomUUID().toString());
        _preview.setSelectedCamera(_callControlService.getDefaultCamera());
        _preview.setPreviewWindow(view);
        _preview.startSelfView();
    }

    public void stopPreview() {
        if (_preview != null) {
            _preview.endSession();
            _preview = null;
        }
    }

    public void dial(@NonNull String dialString, @NonNull CallOption option, @NonNull CompletionHandler<Call> callback) {
        Log.i(TAG, "dial: ->start");
        if (_device == null) {
            Log.e(TAG, "Failure: unregistered device");
            callback.onComplete(Result.error("Failure: unregistered device"));
            return;
        }
        if (_dialCallback != null) {
            Log.w(TAG, "Calling");
            callback.onComplete(Result.error("Calling"));
            return;
        }
        if (_calls.size() > 0) {
            Log.e(TAG, "Failure: There are other active calls");
            callback.onComplete(Result.error("Failure: There are other active calls"));
            return;
        }
        stopPreview();
        _option = option;
        _dialCallback = callback;

        CallContext.Builder builder = new CallContext.Builder(dialString);
        if (!option.hasVideo()) {
            builder = builder.setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioOnly);
        }
        _callControlService.joinCall(builder.build());
        Log.i(TAG, "dial: ->CallImpl sendout");
    }

    void answer(@NonNull CallImpl call, @NonNull CallOption option, @NonNull CompletionHandler<Void> callback) {
        Log.d(TAG, "answer: ->start");
        for (CallImpl exist : _calls.values()) {
            if (!exist.getKey().equals(call.getKey()) && exist.getStatus() == Call.CallStatus.CONNECTED) {
                Log.e(TAG, "There are other active calls");
                callback.onComplete(Result.error("There are other active calls"));
                return;
            }
        }
        if (call.getDirection() == Call.Direction.OUTGOING) {
            Log.e(TAG, "Unsupport function for outgoing call");
            callback.onComplete(Result.error("Unsupport function for outgoing call"));
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
                Log.e(TAG, "Already connected");
                callback.onComplete(Result.error("Already connected"));
                return;
            }
            else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                Log.e(TAG, "Already disconnected");
                callback.onComplete(Result.error("Already disconnected"));
                return;
            }
        }
        stopPreview();
        _option = option;
        _incomingCallback = callback;

        CallContext.Builder builder = new CallContext.Builder(call.getKey()).setIsAnsweringCall(true).setIsOneOnOne(true);
        if (!option.hasVideo()) {
            builder = builder.setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioOnly);
        }
        _callControlService.joinCall(builder.build());
    }

    void reject(@NonNull CallImpl call, @NonNull CompletionHandler<Void> callback) {
        Log.d(TAG, "reject: ->start ");
        if (call.getDirection() == Call.Direction.OUTGOING) {
            Log.e(TAG, "Unsupport function for outgoing call");
            callback.onComplete(Result.error("Unsupport function for outgoing call"));
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
                Log.e(TAG, "Already connected");
                callback.onComplete(Result.error("Already connected"));
                return;
            }
            else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                Log.e(TAG, "Already disconnected");
                callback.onComplete(Result.error("Already disconnected"));
                return;
            }
        }
        _incomingCallback = callback;
        _callControlService.declineCall(call.getKey());
    }

    void hangup(@NonNull CallImpl call, @NonNull CompletionHandler<Void> callback) {
        Log.d(TAG, "hangup -> call " + call.getKey());
        if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
            Log.e(TAG, "Already disconnected");
            callback.onComplete(Result.error("Already disconnected"));
            return;
        }
        _incomingCallback = callback;
        if (call.getStatus() == Call.CallStatus.INITIATED || call.getStatus() == Call.CallStatus.RINGING) {
            _callControlService.cancelCall(call.getKey(), CallControlService.CancelReason.LOCAL_CANCELLED);
        }
        else {
            _callControlService.leaveCall(call.getKey());
        }
    }

    private void _removeCall(@NonNull CallObserver.CallDisconnectedEvent event) {
        CallImpl call = (CallImpl) event.getCall();
        Log.w(TAG, "###############1 " + call);
        if (call != null) {
            call.setStatus(Call.CallStatus.DISCONNECTED);
            CallObserver observer = call.getObserver();
            Log.w(TAG, "###############2 " + observer);
            if (observer != null) {
                observer.onDisconnected(event);
            }
            _calls.remove(call.getKey());
            if (_bus.isRegistered(call)) {
                _bus.unregister(call);
            }
        }
    }

    CallControlService getCallService() {
        return _callControlService;
    }

    OperationQueue getOperationQueue() {
        return _operationQueue;
    }

    public void onEventMainThread(DeviceRegistrationChangedEvent event) {
        Log.i(TAG, "DeviceRegistrationChangedEvent -> is received ");
        if (_registerCallback == null) {
            Log.i(TAG, "this.mRegisterListener is null ");
            return;
        }
        _device = event.getDeviceRegistration();
        _registerCallback.onComplete(Result.success(null));
        _registerCallback = null;
        _registerTimer.removeCallbacks(_registerTimeoutTask);
        Log.i(TAG, "onEventMainThread: Registered:" + event.getDeviceRegistration().getId());
    }

    // Locus has create call,waiting remote to accept
    public void onEventMainThread(CallControlLocusCreatedEvent event) {
        Log.i(TAG, "CallControlLocusCreatedEvent -> is received " + event.getLocusKey());
        LocusKey key = event.getLocusKey();
        CallImpl call = _calls.get(key);
        if (call == null) {
            com.cisco.spark.android.callcontrol.model.Call locus = _callControlService.getCall(key);
            if (locus != null) {
                call = new CallImpl(this, CallImpl.Direction.OUTGOING, key);
                _bus.register(call);
                _calls.put(key, call);
                if (_dialCallback != null) {
                    _dialCallback.onComplete(Result.success(call));
                }
            }
            else {
                Log.e(TAG, "Internal callImpl isn't exist " + event.getLocusKey());
                if (_dialCallback != null) {
                    _dialCallback.onComplete(Result.error("Internal callImpl isn't exist"));
                }
                _option = null;
            }
            _dialCallback = null;

        }
    }

    // Remoted send acknowledge and it means it is RINGING
    public void onEventMainThread(ParticipantNotifiedEvent event) {
        Log.i(TAG, "ParticipantNotifiedEvent -> is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.i(TAG, "Find callImpl " + event.getLocusKey());
            call.setStatus(Call.CallStatus.RINGING);
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onRinging(call);
            }
        }
    }

    // Remote accept call, call will be setup
    public void onEventMainThread(CallControlParticipantJoinedEvent event) {
        Log.i(TAG, "CallControlParticipantJoinedEvent -> is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.i(TAG, "Find callImpl " + event.getLocusKey());
            if (_option != null && _option.hasVideo()) {
                _callControlService.setRemoteWindow(event.getLocusKey(), _option.getRemoteView());
                _callControlService.setPreviewWindow(event.getLocusKey(), _option.getLocalView());
            }
            call.setStatus(Call.CallStatus.CONNECTED);
            if (_incomingCallback != null) {
                _incomingCallback.onComplete(Result.success(null));
                _incomingCallback = null;
            }
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onConnected(call);
            }
        }
    }

    public void onEventMainThread(RetrofitErrorEvent event) {
        Log.i(TAG, "RetrofitErrorEvent is received ");
        clearCallback(Result.error("Error"));
    }

    public void onEventMainThread(CallControlCallJoinErrorEvent event) {
        Log.i(TAG, "CallControlCallJoinErrorEvent is received ");
        clearCallback(Result.error("Join Error"));
    }

    // Local hangup
    public void onEventMainThread(CallControlSelfParticipantLeftEvent event) {
        Log.i(TAG, "CallControlSelfParticipantLeftEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.i(TAG, "Find callImpl " + event.getLocusKey());
            if (_incomingCallback != null) {
                _incomingCallback.onComplete(Result.success(null));
                _incomingCallback = null;
            }
            _removeCall(new CallObserver.LocalLeft(call));
        }
    }

    // Local declined
    public void onEventMainThread(LocusDeclinedEvent event) {
        Log.i(TAG, "CallControlCallDeclinedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            if (_incomingCallback != null) {
                _incomingCallback.onComplete(Result.success(null));
                _incomingCallback = null;
            }
            Log.i(TAG, "Find callImpl " + event.getLocusKey());
            _removeCall(new CallObserver.LocalDecline(call));
        }
    }

    // Local & Remote cancel
    public void onEventMainThread(CallControlCallCancelledEvent event) {
        Log.i(TAG, "CallControlCallCancelledEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.i(TAG, "Find callImpl " + event.getLocusKey());
            if (event.getReason() == CallControlService.CancelReason.REMOTE_CANCELLED) {
                _removeCall(new CallObserver.RemoteCancel(call));
            }
            else {
                if (_incomingCallback != null) {
                    _incomingCallback.onComplete(Result.success(null));
                    _incomingCallback = null;
                }
                _removeCall(new CallObserver.LocalCancel(call));
            }
        }
    }

    // Remote hangup
    public void onEventMainThread(CallControlParticipantLeftEvent event) {
        Log.i(TAG, "CallControlParticipantLeftEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.i(TAG, "Find callImpl " + event.getLocusKey());
            _removeCall(new CallObserver.RemoteLeft(call));
        }
    }

    // Remote declined
    public void onEventMainThread(CallControlLeaveLocusEvent event) {
        Log.i(TAG, "CallControlLeaveLocusEvent is received " + event.locusData().getKey());
        CallImpl call = _calls.get(event.locusData().getKey());
        if (call != null && (!(event.wasMediaFlowing() || event.wasUCCall() || event.wasRoomCall() || event.wasRoomCallConnected()))) {
            Log.i(TAG, "Find callImpl " + event.locusData().getKey());
            _removeCall(new CallObserver.RemoteDecline(call));
        }
    }

    // Incoming Call
    public void onEventMainThread(CallNotificationEvent event) {
        Log.i(TAG, "CallNotificationEvent " + event.getType());
        if (event.getType() == CallNotificationType.INCOMING) {
            Log.i(TAG, "InComing Call");
            CallImpl call = new CallImpl(this, CallImpl.Direction.INCOMING, event.getLocusKey());
            _bus.register(call);
            _calls.put(call.getKey(), call);
            IncomingCallListener listener = getIncomingCallListener();
            if (listener != null) {
                listener.onIncomingCall(call);
            }
        }
    }

    public void onEventMainThread(CallControlLocalAudioMutedEvent event) {
        Log.d(TAG, "CallControlLocalAudioMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.d(TAG, "Find callImpl " + event.getLocusKey());
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onMediaChanged(new CallObserver.SendingAudio(call, !event.isMuted()));
            }
        }
    }

    public void onEventMainThread(CallControlLocalVideoMutedEvent event) {
        Log.d(TAG, "CallControlLocalVideoMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.d(TAG, "Find callImpl " + event.getLocusKey());
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onMediaChanged(new CallObserver.SendingVideo(call, !event.isMuted()));
            }
        }
    }

    public void onEventMainThread(CallControlParticipantAudioMuteEvent event) {
        Log.i(TAG, "CallControlParticipantAudioMuteEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.d(TAG, "Find callImpl " + event.getLocusKey());
            LocusSelfRepresentation self = call.getSelf();
            if (self == null || !self.getUrl().equals(event.getParticipant().getUrl())) {
                CallObserver observer = call.getObserver();
                if (observer != null) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingAudioEvent(call, !event.isMuted()));
                }
            }
            else {
                // for local ??
            }
        }
    }

    public void onEventMainThread(CallControlParticipantVideoMutedEvent event) {
        Log.i(TAG, "CallControlParticipantVideoMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Log.d(TAG, "Find callImpl " + event.getLocusKey());
            LocusSelfRepresentation self = call.getSelf();
            if (self == null || !self.getUrl().equals(event.getParticipant().getUrl())) {
                CallObserver observer = call.getObserver();
                if (observer != null) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingVideoEvent(call, !event.isMuted()));
                }
            }
            else {
                // for local ??
            }
        }
    }

    public void onEventMainThread(CallControlLocusChangedEvent event) {
        Log.i(TAG, "CallControlLocusChangedEvent is received ");
        // DO THIS FOR PSTN/SIP
    }

    public void onEventMainThread(RequestCallingPermissions event) {
        Log.i(TAG, "RequestCallingPermissions -> is received");
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.CAMERA);
        clearCallback(Result.error("Permissions Error"));
    }

    private void clearCallback(Result result) {
        _option = null;
        if (_dialCallback != null) {
            _dialCallback.onComplete(result);
            _dialCallback = null;
        }
        if (_incomingCallback != null) {
            _incomingCallback.onComplete(result);
            _incomingCallback = null;
        }
    }

    static FacingMode toFacingMode(String s) {
        if (MediaEngine.WME_BACK_CAMERA.equals(s)) {
            return FacingMode.ENVIROMENT;
        }
        return FacingMode.USER;
    }

    static String fromFacingMode(FacingMode mode) {
        if (mode == FacingMode.ENVIROMENT) {
            return MediaEngine.WME_BACK_CAMERA;
        }
        return MediaEngine.WME_FRONT_CAMERA;
    }
}

