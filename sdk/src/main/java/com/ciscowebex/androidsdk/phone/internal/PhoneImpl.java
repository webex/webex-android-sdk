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

import java.util.*;
import javax.inject.Inject;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.events.*;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.events.ApplicationControllerStateChangedEvent;
import com.cisco.spark.android.events.CallNotificationEvent;
import com.cisco.spark.android.events.CallNotificationType;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.events.RequestCallingPermissions;
import com.cisco.spark.android.locus.events.*;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantDevice;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.responses.LocusUrlResponse;
import com.cisco.spark.android.media.MediaCapabilityConfig;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.media.events.MediaAvailabilityChangeEvent;
import com.cisco.spark.android.media.events.MediaBlockedChangeEvent;
import com.cisco.spark.android.metrics.CallAnalyzerReporter;
import com.cisco.spark.android.sync.operationqueue.core.Operations;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wme.appshare.ScreenShareContext;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.AcquirePermissionActivity;
import com.ciscowebex.androidsdk.internal.MetricsClient;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.people.Person;
import com.ciscowebex.androidsdk.people.internal.PersonClientImpl;
import com.ciscowebex.androidsdk.phone.AuxStream;
import com.ciscowebex.androidsdk.phone.Call;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.CallObserver;
import com.ciscowebex.androidsdk.phone.MediaOption;
import com.ciscowebex.androidsdk.phone.MultiStreamObserver;
import com.ciscowebex.androidsdk.phone.Phone;
import com.ciscowebex.androidsdk.utils.Utils;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.ciscowebex.androidsdk_commlib.SDKCommon;
import com.github.benoitdion.ln.Ln;

import com.google.common.collect.Lists;
import me.helloworld.utils.Checker;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.NoSubscriberEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import retrofit2.Callback;
import retrofit2.Response;

public class PhoneImpl implements Phone {

    @Inject
    ApplicationController _applicationController;

    @Inject
    ApiClientProvider apiClientProvider;

    @Inject
    CallControlService _callControlService;

    @Inject
    Operations _operations;

    @Inject
    MediaEngine _mediaEngine;

    @Inject
    EventBus _bus;

    @Inject
    CallAnalyzerReporter _callAnalyzerReporter;

    @Inject
    volatile Settings settings;

    private IncomingCallListener _incomingCallListener;

    private Authenticator _authenticator;

    private DeviceRegistration _device;

    private Handler _registerTimer;

    public Handler getHandler() {
        return _registerTimer;
    }

    private Runnable _registerTimeoutTask;

    private CompletionHandler<Void> _registerCallback;

    private Map<LocusKey, CallImpl> _calls = new HashMap<>();

    private Map<String, String> _callTags = new HashMap<>();

    public CallImpl getCall(LocusKey key) {
        return _calls.get(key);
    }

    private CompletionHandler<Call> _dialCallback;

    private MediaOption _dialOption;

    private MediaSession _preview;

    private H264LicensePrompter _prompter;

    private MetricsClient _metrics;

    private Context _context;

    private Intent _screenSharingIntent;

    private LocusKey _screenSharingKey;

    private LocusParticipant _lostSharingParticipant;

    private Uri _currentSharingUri;

    private boolean _isRemoteSendingVideo;

    public boolean getRemoteSendingVideo() {
        return _isRemoteSendingVideo;
    }

    private boolean _isRemoteSendingAudio;

    private LocusKey _activeCallLocusKey;

    private int _availableMediaCount;

    private int audioMaxBandwidth = DefaultBandwidth.MAX_BANDWIDTH_AUDIO.getValue();

    private int videoMaxBandwidth = DefaultBandwidth.MAX_BANDWIDTH_720P.getValue();

    private int sharingMaxBandwidth = DefaultBandwidth.MAX_BANDWIDTH_SESSION.getValue();

    private boolean isEnableHardwareAcceleration = true;

    private String hardwareVideoSettings = null;

    private static final String STR_PERMISSION_DENIED = "permission deined";
    private static final String STR_OTHER_ACTIVE_CALLS = "There are other active calls";
    private static final String STR_UNSUPPORTED_FOR_OUTGOING_CALL = "Unsupport function for outgoing call";
    private static final String STR_ALREADY_CONNECTED = "Already connected";
    private static final String STR_ALREADY_DISCONNECTED = "Already disconnected";
    private static final String STR_FIND_CALLIMPL = "Find callImpl ";
    private static final String STR_FAILURE_CALL = "Failure call: ";

    private static class DialTarget {
        private String address;
        private AddressType type;

        enum AddressType {
            PEOPLE_ID,
            PEOPLE_MAIL,
            SPACE_ID,
            SPACE_MAIL,
            OTHER
        }

        public boolean isEndpoint() {
            switch (this.type) {
                case OTHER:
                case PEOPLE_ID:
                case SPACE_MAIL:
                case PEOPLE_MAIL:
                    return true;
                case SPACE_ID:
                    return false;
                default:
                    return true;
            }
        }

        public boolean isGroup() {
            switch (this.type) {
                case OTHER:
                case PEOPLE_ID:
                case PEOPLE_MAIL:
                    return false;
                case SPACE_MAIL:
                case SPACE_ID:
                    return true;
                default:
                    return false;
            }
        }

        public DialTarget(@NonNull String address) {
            this.address = address;
            lookup();
        }

        private void lookup() {
            String target = this.parseHydraId(this.address);
            if (!Checker.isEmpty(target)) {
                this.address = target;
            } else if (this.address.toLowerCase().endsWith("@meet.ciscospark.com")) {
                this.type = AddressType.SPACE_MAIL;
            } else if (this.address.contains("@") && !this.address.contains(".")) {
                this.type = AddressType.PEOPLE_MAIL;
            } else {
                this.type = AddressType.OTHER;
            }
        }

        private String parseHydraId(String id) {
            try {
                byte[] bytes = Base64.decode(id, Base64.URL_SAFE);
                if (Checker.isEmpty(bytes)) {
                    return null;
                }
                String decode = new String(bytes, "UTF-8");
                Uri uri = Uri.parse(decode);
                if (uri != null && uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("ciscospark")) {
                    List<String> paths = uri.getPathSegments();
                    if (paths != null && paths.size() >= 2) {
                        String aType = paths.get(paths.size() - 2);
                        if (aType.equalsIgnoreCase("PEOPLE")) {
                            this.type = AddressType.PEOPLE_ID;
                            return paths.get(paths.size() - 1);
                        } else if (aType.equalsIgnoreCase("ROOM") || aType.equalsIgnoreCase("SPACE")) {
                            this.type = AddressType.SPACE_ID;
                            return paths.get(paths.size() - 1);
                        }
                    }
                }
                return null;
            } catch (Exception t) {
                return null;
            }

        }

    }

    public PhoneImpl(Context context, Authenticator authenticator, SDKCommon common) {
        common.inject(this);
        _authenticator = authenticator;
        _bus.register(this);
        _registerTimer = new Handler();
        _context = context;
        _prompter = new H264LicensePrompter(context.getSharedPreferences(Webex.class.getPackage().getName(), Context.MODE_PRIVATE));
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
        _prompter.check(builder, result -> callback.onComplete(ResultImpl.success(result)));
    }

    public String getVideoCodecLicense() {
        return _prompter.getLicense();
    }

    public String getVideoCodecLicenseURL() {
        return _prompter.getLicenseURL();
    }

    @Override
    public void setAudioMaxBandwidth(int bandwidth) {
        audioMaxBandwidth = bandwidth <= 0 ? DefaultBandwidth.MAX_BANDWIDTH_AUDIO.getValue() : bandwidth;
    }

    @Override
    public int getAudioMaxBandwidth() {
        return audioMaxBandwidth;
    }

    @Override
    public void setVideoMaxBandwidth(int bandwidth) {
        videoMaxBandwidth = bandwidth <= 0 ? DefaultBandwidth.MAX_BANDWIDTH_720P.getValue() : bandwidth;
    }

    @Override
    public int getVideoMaxBandwidth() {
        return videoMaxBandwidth;
    }

    @Override
    public void setSharingMaxBandwidth(int bandwidth) {
        sharingMaxBandwidth = bandwidth <= 0 ? DefaultBandwidth.MAX_BANDWIDTH_SESSION.getValue() : bandwidth;
    }

    @Override
    public int getSharingMaxBandwidth() {
        return sharingMaxBandwidth;
    }

    @Override
    public boolean isHardwareAccelerationEnabled() {
        return isEnableHardwareAcceleration;
    }

    @Override
    public void setHardwareAccelerationEnabled(boolean enable) {
        isEnableHardwareAcceleration = enable;
    }

    public String getHardwareVideoSettings() {
        return hardwareVideoSettings;
    }

    public void setHardwareVideoSettings(String settings) {
        hardwareVideoSettings = settings;
    }

    @Override
    public void enableAudioEnhancementForModels(List<String> models) {
        if (_callControlService != null) {
            _callControlService.setAudioEnhancementModels(models);
        }
    }

    public void disableVideoCodecActivation() {
        _prompter.setVideoLicenseActivationDisabled(true);
    }

    public void register(@NonNull CompletionHandler<Void> callback) {
        Ln.i("Registering");
        if (_registerCallback != null) {
            Ln.w("Already registering");
            callback.onComplete(ResultImpl.error("Already registering"));
            return;
        }
        _registerCallback = callback;
        RotationHandler.registerRotationReceiver(_context, this);

        ServiceBuilder.async(_authenticator, callback, true, s -> {
            if (s == null) {
                RotationHandler.unregisterRotationReceiver(_context);
                _registerCallback = null;
            }
            else {
                _registerTimeoutTask = () -> {
                    Ln.i("Register timeout");
                    if (_device == null && _registerCallback != null) {
                        _registerCallback.onComplete(ResultImpl.error("Register timeout"));
                    }
                };
                _registerTimer.postDelayed(_registerTimeoutTask, 60L * 1000);
                new AuthenticatedUserTask(_applicationController).execute();
            }
            return null;
        }, null);
    }

    public void deregister(@NonNull CompletionHandler<Void> callback) {
        Ln.i("Deregistering");
        resetDialStatus(null);
        RotationHandler.unregisterRotationReceiver(_context);
        _applicationController.logout(null, false, false, false);
        _mediaEngine.uninitialize();
        _device = null;
        _registerCallback = null;
        _registerTimer.removeCallbacks(_registerTimeoutTask);
        callback.onComplete(ResultImpl.success(null));
        Ln.i("Deregistered");
    }

    public void startPreview(View view) {
        stopPreview();
        _preview = _mediaEngine.createMediaSession(UUID.randomUUID().toString());
        if (!_preview.getSelectedCamera().equals(_callControlService.getDefaultCamera())) {
            _preview.switchCamera();
        }
        _preview.setPreviewWindow(view);
        _preview.startSelfView();
        setDisplayRotation(RotationHandler.getRotation(_context));
    }

    public void stopPreview() {
        if (_preview != null) {
            _preview.endSession();
            _preview = null;
        }
    }

    public void setDisplayRotation(int rotation) {
        if (_preview != null) {
            _preview.setDisplayRotation(rotation);
        }
        for (CallImpl call : _calls.values()) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
                _callControlService.setDisplayRotation(call.getKey(), rotation);
                return;
            }
        }
    }

    void makeCall(Bundle data, boolean permission) {
        if (data == null) {
            Ln.e("makeCall data is null!");
            return;
        }
        String tag = data.getString(AcquirePermissionActivity.CALL_TAG);
        if (tag == null) {
            Ln.w("Duplicated call, ignore it");
            return;
        }
        if (!_callTags.containsKey(tag)) {
            Ln.w("Not found call tag, ignore it");
            return;
        }
        int direction = data.getInt(AcquirePermissionActivity.CALL_DIRECTION);
        if (direction == Call.Direction.INCOMING.ordinal()) {
            Ln.d("make incoming call");
            LocusKey key = data.getParcelable(AcquirePermissionActivity.CALL_KEY);
            CallImpl call = _calls.get(key);
            if (call != null) {
                Result<Void> result = null;
                if (permission) {
                    CallContext.Builder builder = new CallContext.Builder(call.getKey()).setIsAnsweringCall(true).setIsOneOnOne(!call.isGroup());
                    builder = builder.setMediaDirection(mediaOptionToMediaDirection(call.getOption()));
                    if (!doDial(builder.build())) {
                        result = ResultImpl.error(STR_FAILURE_CALL + "cannot dial");
                    }
                } else {
                    Ln.w(STR_PERMISSION_DENIED);
                    result = ResultImpl.error(STR_PERMISSION_DENIED);
                }
                CompletionHandler<Void> handler = call.getAnswerCallback();
                if (handler != null && result != null) {
                    handler.onComplete(result);
                }
            }
            else {
                Ln.d("Cannot find call for key: " + key);
            }
        } else if (direction == Call.Direction.OUTGOING.ordinal()) {
            Ln.d("make outgoing call");
            String dialString = data.getString(AcquirePermissionActivity.CALL_STRING);
            if (permission && dialString != null) {
                DialTarget target = new DialTarget(dialString);
                if (target.type == DialTarget.AddressType.PEOPLE_MAIL) {
                    new PersonClientImpl(_authenticator).list(dialString, null, 1, result -> {
                        List<Person> persons = result.getData();
                        if (!Checker.isEmpty(persons)) {
                            Person person = persons.get(0);
                            Ln.d("Lookup target: " + person.getId());
                            doDial(new DialTarget(person.getId()).address, _dialOption);
                        } else {
                            doDial(target.address, _dialOption);
                        }
                    });
                } else if (target.isEndpoint()) {
                    doDial(target.address, _dialOption);
                } else {
                    doDialSpaceID(target.address, _dialOption);
                }
            } else {
                Ln.w(STR_PERMISSION_DENIED);
                resetDialStatus(ResultImpl.error(STR_PERMISSION_DENIED));
            }
        }
        _callTags.remove(tag);
    }

    public void dial(@NonNull String dialString, @NonNull MediaOption option, @NonNull CompletionHandler<Call> callback) {
        Ln.i("Dialing: " + dialString + ", " + option.hasVideo());
        if (_device == null) {
            Ln.e("Unregistered device");
            callback.onComplete(ResultImpl.error("Unregistered device"));
            return;
        }
        if (_dialCallback != null) {
            Ln.w("Already calling");
            callback.onComplete(ResultImpl.error("Already calling"));
            return;
        }
        for (CallImpl call : _calls.values()) {
            if (!call.isGroup() || (call.isGroup() && call.getStatus() == Call.CallStatus.CONNECTED)) {
                Ln.e(STR_OTHER_ACTIVE_CALLS);
                callback.onComplete(ResultImpl.error(STR_OTHER_ACTIVE_CALLS));
                return;
            }
        }
        stopPreview();
        _dialOption = option;
        _dialCallback = callback;
        tryAcquirePermission(Call.Direction.OUTGOING, dialString);
    }

    void answer(@NonNull CallImpl call) {
        Ln.i("Answer " + call);
        for (CallImpl exist : _calls.values()) {
            Ln.d("answer exist.getStatus(): " + exist.getStatus());
            if (!exist.getKey().equals(call.getKey()) && exist.getStatus() == Call.CallStatus.CONNECTED) {
                Ln.e(STR_OTHER_ACTIVE_CALLS);
                if (call.getAnswerCallback() != null) {
                    call.getAnswerCallback().onComplete(ResultImpl.error(STR_OTHER_ACTIVE_CALLS));
                }
                return;
            }
        }
        if (call.getDirection() == Call.Direction.OUTGOING) {
            Ln.e(STR_UNSUPPORTED_FOR_OUTGOING_CALL);
            if (call.getAnswerCallback() != null) {
                call.getAnswerCallback().onComplete(ResultImpl.error(STR_UNSUPPORTED_FOR_OUTGOING_CALL));
            }
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
                Ln.e(STR_ALREADY_CONNECTED);
                if (call.getAnswerCallback() != null) {
                    call.getAnswerCallback().onComplete(ResultImpl.error(STR_ALREADY_CONNECTED));
                }
                return;
            } else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                Ln.e(STR_ALREADY_DISCONNECTED);
                if (call.getAnswerCallback() != null) {
                    call.getAnswerCallback().onComplete(ResultImpl.error(STR_ALREADY_DISCONNECTED));
                }
                return;
            }
        }
        stopPreview();
        tryAcquirePermission(Call.Direction.INCOMING, call.getKey());
    }

    private void tryAcquirePermission(Call.Direction direction, Object callParam) {
        String tag = UUID.randomUUID().toString();
        _callTags.put(tag, tag);

        Bundle bundle = new Bundle();
        bundle.putString(AcquirePermissionActivity.CALL_TAG, tag);
        bundle.putInt(AcquirePermissionActivity.CALL_DIRECTION, direction.ordinal());
        if (callParam instanceof Parcelable) {
            bundle.putParcelable(AcquirePermissionActivity.CALL_KEY, (Parcelable) callParam);
        }
        else if (callParam instanceof String) {
            bundle.putString(AcquirePermissionActivity.CALL_STRING, (String) callParam);
        }
        final Intent intent = new Intent(_context, AcquirePermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquirePermissionActivity.PERMISSION_TYPE, AcquirePermissionActivity.PERMISSION_CAMERA_MIC);
        intent.putExtra(AcquirePermissionActivity.CALL_DATA, bundle);
        _context.startActivity(intent);
    }

    void reject(@NonNull CallImpl call) {
        Ln.i("Reject " + call);
        if (call.getDirection() == Call.Direction.OUTGOING) {
            Ln.e(STR_UNSUPPORTED_FOR_OUTGOING_CALL);
            if (call.getRejectCallback() != null) {
                call.getRejectCallback().onComplete(ResultImpl.error(STR_UNSUPPORTED_FOR_OUTGOING_CALL));
            }
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
                Ln.e(STR_ALREADY_CONNECTED);
                if (call.getRejectCallback() != null) {
                    call.getRejectCallback().onComplete(ResultImpl.error(STR_ALREADY_CONNECTED));
                }
                return;
            } else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                Ln.e(STR_ALREADY_DISCONNECTED);
                if (call.getRejectCallback() != null) {
                    call.getRejectCallback().onComplete(ResultImpl.error(STR_ALREADY_DISCONNECTED));
                }
                return;
            }
        }
        _callControlService.declineCall(call.getKey());
    }

    void hangup(@NonNull CallImpl call) {
        Ln.i("Hangup " + call);
        resetDialStatus(null);
        if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
            Ln.e(STR_ALREADY_DISCONNECTED);
            if (call.getHangupCallback() != null) {
                call.getHangupCallback().onComplete(ResultImpl.error(STR_ALREADY_DISCONNECTED));
            }
            return;
        }
        if (call.getStatus() == Call.CallStatus.INITIATED || call.getStatus() == Call.CallStatus.RINGING) {
            _callControlService.cancelCall(call.getKey(), CallControlService.CancelReason.LOCAL_CANCELLED);
        } else {
            _callControlService.leaveCall(call.getKey());
        }
    }

    void sendFeedback(Map<String, String> feedback) {
        if (_metrics != null) {
            feedback.put("key", "meetup_call_user_rating");
            feedback.put("time", Utils.timestampUTC());
            feedback.put("type", "GENERIC");
            List<Map<String, String>> list = new ArrayList<>();
            list.add(feedback);
            _metrics.post(list);
        }
    }

    CallControlService getCallService() {
        return _callControlService;
    }

    Operations getOperations() {
        return _operations;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DeviceRegistrationChangedEvent event) {
        Ln.i("DeviceRegistrationChangedEvent is received ");
        _device = settings.getDeviceRegistration();
        Uri uri = _device.getMetricsServiceUrl();
        if (uri != null) {
            String url = uri.toString();
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            _metrics = new MetricsClient(_authenticator, url);
        }
        if (_registerCallback == null) {
            Ln.w("Register callback is null ");
            return;
        }
        _registerCallback.onComplete(ResultImpl.success(null));
        _registerCallback = null;
        _registerTimer.removeCallbacks(_registerTimeoutTask);
        Ln.i("Registered: " + _device.getId());
    }

    // Locus has create call,waiting remote to accept
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlLocusCreatedEvent event) {
        Ln.i("CallControlLocusCreatedEventis received " + event.getLocusKey());
        LocusKey key = event.getLocusKey();
        CallImpl call = _calls.get(key);
        if (call == null) {
            com.cisco.spark.android.callcontrol.model.Call locus = _callControlService.getCall(key);
            if (locus == null) {
                Ln.e("Internal callImpl isn't exist " + event.getLocusKey());
                resetDialStatus(ResultImpl.error("Internal callImpl isn't exist"));
            } else {
                call = new CallImpl(this, _dialOption, CallImpl.Direction.OUTGOING, key, locus.getLocusData().isMeeting());
                Ln.d("Call is created: " + call);
                _bus.register(call);
                _calls.put(key, call);
                if (_dialCallback != null) {
                    _dialCallback.onComplete(ResultImpl.success(call));
                    _dialCallback = null;
                }
                if (call.isGroup()) {
                    setCallOnRinging(call);
                    setCallOnConnected(call, event.getLocusKey());
                }
            }
        } else {
            Ln.e("Internal callImpl is exist " + call);
            resetDialStatus(null);
        }
    }

    // Remoted send acknowledge and it means it is RINGING
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ParticipantNotifiedEvent event) {
        Ln.i("ParticipantNotifiedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            if (call.isGroup()) {
                if (call.getDirection() == Call.Direction.INCOMING) {
                    setCallOnRinging(call);
                }
            } else {
                if (call.getDirection() == Call.Direction.INCOMING) {
                    for (LocusParticipant participant : call.getRemoteParticipants()) {
                        Ln.d("participant State: " + participant.getState());
                        if (participant.getState() == LocusParticipant.State.JOINED) {
                            setCallOnRinging(call);
                            break;
                        }
                    }
                } else {
                    setCallOnRinging(call);
                }
            }
        }
    }

    // Remote accept call, call will be setup
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlParticipantJoinedEvent event) {
        Ln.i("CallControlParticipantJoinedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call == null) {
            //group call self join.
            com.cisco.spark.android.callcontrol.model.Call locus = _callControlService.getCall(event.getLocusKey());
            for (LocusParticipant locusParticipant : event.getJoinedParticipants()) {
                if (locusParticipant.getDeviceUrl().equals(_device.getUrl())
                        && locus != null
                        && locus.getLocusData().isMeeting()) {
                    call = new CallImpl(this, _dialOption, CallImpl.Direction.OUTGOING, event.getLocusKey(), locus.getLocusData().isMeeting());
                    _bus.register(call);
                    _calls.put(call.getKey(), call);
                    if (_dialCallback != null) {
                        _dialCallback.onComplete(ResultImpl.success(call));
                        _dialCallback = null;
                    }
                    setCallOnRinging(call);
                    setCallOnConnected(call, event.getLocusKey());
                }
            }
        } else {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            if (!call.isGroup()) {
                resetDialStatus(null);
                if (call.getAnswerCallback() != null && (call.getStatus() == Call.CallStatus.INITIATED || call.getStatus() == Call.CallStatus.RINGING)) {
                    call.getAnswerCallback().onComplete(ResultImpl.success(null));
                }
                setCallOnConnected(call, event.getLocusKey());
            } else if (call.getStatus() != Call.CallStatus.CONNECTED) {
                for (LocusParticipant locusParticipant : event.getJoinedParticipants()) {
                    if (locusParticipant != null && locusParticipant.getDeviceUrl() != null && locusParticipant.getDeviceUrl().equals(_device.getUrl())) {
                        if (call.getAnswerCallback() != null) {
                            call.getAnswerCallback().onComplete(ResultImpl.success(null));
                        }
                        if (_dialCallback != null) {
                            _dialCallback.onComplete(ResultImpl.success(call));
                            _dialCallback = null;
                        }
                        setCallOnRinging(call);
                        if (call.getOption() == null && _dialOption != null) {
                            call.setMediaOption(_dialOption);
                        }
                        setCallOnConnected(call, event.getLocusKey());
                    }
                }
                resetDialStatus(null);
            } else {
                resetDialStatus(null);
            }
        }
        //call membership changed
        List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
        for (LocusParticipant locusParticipant : event.getJoinedParticipants()) {
            if (locusParticipant != null && locusParticipant.getDeviceUrl() != null && !locusParticipant.getDeviceUrl().equals(_device.getUrl())) {
                events.add(new CallObserver.MembershipJoinedEvent(call, new CallMembershipImpl(locusParticipant, call)));
            }
        }
        doCallMembershipChanged(call, events);
        doParticipantCountChanged(call);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RetrofitErrorEvent event) {
        Ln.e("RetrofitErrorEvent is received");
        resetDialStatus(ResultImpl.error("Retrofit error: " + Utils.toMap(event)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlCallJoinErrorEvent event) {
        Ln.e("CallControlCallJoinErrorEvent is received ");
        resetDialStatus(ResultImpl.error("Join Error: " + Utils.toMap(event)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConflictErrorJoiningLocusEvent event) {
        Ln.e("ConflictErrorJoiningLocusEvent is received ");
        resetDialStatus(ResultImpl.error("Join Error: " + Utils.toMap(event)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(HighVolumeErrorJoiningLocusEvent event) {
        Ln.e("HighVolumeErrorJoiningLocusEvent is received ");
        resetDialStatus(ResultImpl.error("Join Error: " + Utils.toMap(event)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LocusUserIsNotAuthorized event) {
        Ln.e("LocusUserIsNotAuthorized is received ");
        resetDialStatus(ResultImpl.error("Join Error: " + Utils.toMap(event)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LocusInviteesExceedMaxSizeEvent event) {
        Ln.e("LocusInviteesExceedMaxSizeEvent is received ");
        resetDialStatus(ResultImpl.error("Join Error: " + Utils.toMap(event)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LocusMeetingLockedEvent event) {
        Ln.e("LocusMeetingLockedEvent is received ");
        resetDialStatus(ResultImpl.error("Join Error: " + Utils.toMap(event)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(InvalidLocusEvent event) {
        Ln.e("InvalidLocusEvent is received ");
        resetDialStatus(ResultImpl.error("Join Error: " + Utils.toMap(event)));
    }

    // Local hangup
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlSelfParticipantLeftEvent event) {
        Ln.i("CallControlSelfParticipantLeftEvent is received " + event.getLocusKey());
        resetDialStatus(null);
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            if (call.getHangupCallback() != null) {
                call.getHangupCallback().onComplete(ResultImpl.success(null));
            }
            removeCall(new CallObserver.LocalLeft(call));
        }
    }

    // Local declined
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LocusDeclinedEvent event) {
        Ln.i("LocusDeclinedEvent is received " + event.getLocusKey());
        resetDialStatus(null);
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            if (call.getRejectCallback() != null) {
                call.getRejectCallback().onComplete(ResultImpl.success(null));
            }
            removeCall(new CallObserver.LocalDecline(call));
        }
    }

    // Local & Remote cancel
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlCallCancelledEvent event) {
        Ln.i("CallControlCallCancelledEvent is received " + event.getLocusKey());
        resetDialStatus(null);
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            if (event.getReason() == CallControlService.CancelReason.REMOTE_CANCELLED) {
                removeCall(new CallObserver.RemoteCancel(call));
            } else {
                if (call.getHangupCallback() != null) {
                    call.getHangupCallback().onComplete(ResultImpl.success(null));
                }
                removeCall(new CallObserver.LocalCancel(call));
            }
        }
    }

    // Remote hangup
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlParticipantLeftEvent event) {
        Ln.i("CallControlParticipantLeftEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            if (!call.isGroup()) {
                resetDialStatus(null);
                removeCall(new CallObserver.RemoteLeft(call));
            } else if (call.getStatus() == Call.CallStatus.INITIATED || call.getStatus() == Call.CallStatus.RINGING) {
                boolean meetingIsOpen = false;
                for (CallMembership membership : call.getMemberships()) {
                    if (membership.getState() == CallMembership.State.JOINED) {
                        meetingIsOpen = true;
                    }
                }
                if (!meetingIsOpen) {
                    resetDialStatus(null);
                    removeCall(new CallObserver.RemoteCancel(call));
                }
            }
            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            for (LocusParticipant locusParticipant : event.getLeftParticipants()) {
                events.add(new CallObserver.MembershipLeftEvent(call, new CallMembershipImpl(locusParticipant, call)));
            }
            doCallMembershipChanged(call, events);
            doParticipantCountChanged(call);
        }
    }

    // Remote declined
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlLeaveLocusEvent event) {
        Ln.i("CallControlLeaveLocusEvent is received " + event.locusData().getKey());
        resetDialStatus(null);
        CallImpl call = _calls.get(event.locusData().getKey());
        // if (call != null && (event.wasCallDeclined() && !(event.wasMediaFlowing() || event.wasUCCall() || event.wasRoomCall() || event.wasRoomCallConnected()))) {
        if (call == null) {
            Ln.d("Cannot find the call " + event.locusData().getKey());
        } else {
            Ln.d(STR_FIND_CALLIMPL + event.locusData().getKey());
            removeCall(new CallObserver.RemoteDecline(call));
        }
    }

    // Space remote declined
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ParticipantRoomDeclinedEvent event) {
        Ln.i("ParticipantRoomDeclinedEvent is received " + event.getLocusKey());
        resetDialStatus(null);
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            for (LocusParticipant locusParticipant : event.getDeclinedParticipants()) {
                events.add(new CallObserver.MembershipDeclinedEvent(call, new CallMembershipImpl(locusParticipant, call)));
            }
            doCallMembershipChanged(call, events);
        }

    }

    // Incoming Call
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallNotificationEvent event) {
        Ln.i("CallNotificationEvent is received " + event.getType());
        if (event.getType() == CallNotificationType.INCOMING) {
            Ln.i("InComing Call " + event.getLocusKey());
            LocusData locusData = _callControlService.getLocusData(event.getLocusKey());
            boolean isGroup = locusData != null && locusData.isMeeting();
            CallImpl call = new CallImpl(this, null, CallImpl.Direction.INCOMING, event.getLocusKey(), isGroup);
            _bus.register(call);
            _calls.put(call.getKey(), call);
            IncomingCallListener listener = getIncomingCallListener();
            if (listener != null) {
                listener.onIncomingCall(call);
            }
        }
    }

    // mutiple device hangup
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ParticipantSelfChangedEvent event) {
        Ln.i("ParticipantSelfChangedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        Locus locus = event.getLocus();
        if (call != null && locus != null && locus.getSelf() != null) {
            Uri deviceUrl = locus.getSelf().getDeviceUrl();
            Ln.d("ParticipantSelfChangedEvent device url: " + deviceUrl + "  self: " + _device.getUrl() + "  state: " + locus.getSelf().getState());
            if (call.getStatus() == Call.CallStatus.CONNECTED && !isJoinedFromThisDevice(locus.getSelf().getDevices())) {
                Ln.d("Local device left locusKey: " + event.getLocusKey());
                resetDialStatus(null);
                if (call.getHangupCallback() != null) {
                    call.getHangupCallback().onComplete(ResultImpl.success(null));
                }
                removeCall(new CallObserver.LocalLeft(call));
            } else if (call.getStatus() != Call.CallStatus.CONNECTED
                    && deviceUrl != null && !deviceUrl.equals(_device.getUrl())
                    && locus.getSelf().getState() == LocusParticipant.State.JOINED) {
                com.cisco.spark.android.callcontrol.model.Call aCall = _callControlService.getCall(event.getLocusKey());
                if (aCall == null || !aCall.isActive()) {
                    Ln.d("other device connected locusKey: " + event.getLocusKey());
                    resetDialStatus(null);
                    removeCall(new CallObserver.OtherConnected(call));
                } else {
                    Ln.d("Self device has already connected, ignore other device connect");
                }
            } else if (call.getStatus() != Call.CallStatus.CONNECTED
                    && deviceUrl != null && !deviceUrl.equals(_device.getUrl())
                    && locus.getSelf().getState() == LocusParticipant.State.DECLINED) {
                com.cisco.spark.android.callcontrol.model.Call aCall = _callControlService.getCall(event.getLocusKey());
                if (aCall == null || !aCall.isActive()) {
                    Ln.d("other device declined locusKey: " + event.getLocusKey());
                    resetDialStatus(null);
                    removeCall(new CallObserver.OtherDeclined(call));
                } else {
                    Ln.d("Self device has already connected, ignore other device decline");
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DismissCallNotificationEvent event) {
        Ln.i("DismissCallNotificationEvent is received " + event.getLocusKey());
        final LocusData call = _callControlService.getLocusData(event.getLocusKey());
        if (call != null) {
            call.setIsToasting(false);
        }
        _callAnalyzerReporter.reportCallAlertRemoved(event.getLocusKey());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlLocalAudioMutedEvent event) {
        Ln.i("CallControlLocalAudioMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onMediaChanged(new CallObserver.SendingAudio(call, !event.isMuted()));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlLocalVideoMutedEvent event) {
        Ln.i("CallControlLocalVideoMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onMediaChanged(new CallObserver.SendingVideo(call, !event.isMuted()));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlParticipantAudioMuteEvent event) {
        Ln.i("CallControlParticipantAudioMuteEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            LocusSelfRepresentation self = call.getSelf();
            if (self == null || !self.getUrl().equals(event.getParticipant().getUrl())) {
                CallObserver observer = call.getObserver();
                boolean isSending = call.isRemoteSendingAudio();
                Ln.d("_isRemoteSendingAudio: " + _isRemoteSendingAudio + "  isSending: " + isSending);
                if (observer != null && _isRemoteSendingAudio != isSending) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingAudioEvent(call, isSending));
                    _isRemoteSendingAudio = isSending;
                }
            } else {
                // TODO for local ??
            }
            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            events.add(new CallObserver.MembershipSendingAudioEvent(call, new CallMembershipImpl(event.getParticipant(), call)));
            doCallMembershipChanged(call, events);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlParticipantVideoMutedEvent event) {
        Ln.i("CallControlRemoteVideoMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d(STR_FIND_CALLIMPL + event.getLocusKey());
            LocusSelfRepresentation self = call.getSelf();
            if (!call.isGroup() && (self == null || !self.getUrl().equals(event.getParticipant().getUrl()))) {
                CallObserver observer = call.getObserver();
                boolean isSending = !event.isMuted();
                Ln.d("_isRemoteSendingVideo: " + _isRemoteSendingVideo + "  isSending: " + isSending);
                if (observer != null && _isRemoteSendingVideo != isSending) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingVideoEvent(call, isSending));
                }
                _isRemoteSendingVideo = isSending;
            }

            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            events.add(new CallObserver.MembershipSendingVideoEvent(call, new CallMembershipImpl(event.getParticipant(), call)));
            doCallMembershipChanged(call, events);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlLocusChangedEvent event) {
        Ln.i("CallControlLocusChangedEvent is received ");
        // TODO DO THIS FOR PSTN/SIP
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RequestCallingPermissions event) {
        Ln.i("RequestCallingPermissions is received");
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.CAMERA);
        resetDialStatus(ResultImpl.error(new WebexError<>(WebexError.ErrorCode.PERMISSION_ERROR, "Permissions Error", permissions)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlFloorGrantedEvent event) {
        Ln.i("CallControlFloorGrantedEvent is received ");
        List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            LocusParticipant beneficiary = _callControlService.getLocus(event.getLocusKey()).getFloorBeneficiary();
            if (beneficiary != null) {
                _currentSharingUri = beneficiary.getDeviceUrl();
            }
            for (CallMembership membership : call.getMemberships()) {
                if (membership.isSendingSharing()) {
                    events.add(new CallObserver.MembershipSendingSharingEvent(call, membership));
                    break;
                }
            }
            doCallMembershipChanged(call, events);

            CallObserver observer = call.getObserver();
            if (observer != null) {
                if (call.isSendingSharing()) {
                    observer.onMediaChanged(new CallObserver.SendingSharingEvent(call, true));
                } else if (call.isRemoteSendingSharing() && _lostSharingParticipant == null) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingSharingEvent(call, true));
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlFloorReleasedEvent event) {
        Ln.i("CallControlFloorReleasedEvent is received ");
        List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
        CallImpl call = _calls.get(event.getLocusKey());
        LocusData locusData = _callControlService.getLocusData(event.getLocusKey());
        LocusParticipant beneficiary = locusData.getReleasedParticipantSharing();
        if (beneficiary == null) {
            Ln.w("getReleasedParticipantSharing is null!");
            beneficiary = _lostSharingParticipant;
        }
        if (call != null && locusData != null && beneficiary != null) {
            for (CallMembership membership : call.getMemberships()) {
                if (beneficiary.getPerson() != null
                        && beneficiary.getPerson().getId() != null
                        && membership.getPersonId().equals(Utils.encode(beneficiary.getPerson().getId()))) {
                    events.add(new CallObserver.MembershipSendingSharingEvent(call, membership));
                    break;
                }
            }
            doCallMembershipChanged(call, events);

            CallObserver observer = call.getObserver();
            if (observer != null) {
                if (_currentSharingUri != null && _currentSharingUri.equals(_device.getUrl())) {
                    observer.onMediaChanged(new CallObserver.SendingSharingEvent(call, false));
                    _lostSharingParticipant = null;
                } else if (!locusData.isFloorGranted() || isSharingFromThisDevice(locusData)) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingSharingEvent(call, false));
                    _lostSharingParticipant = null;
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // MultiStream
    // ------------------------------------------------------------------------
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MediaBlockedChangeEvent event) {
        Ln.d("MediaBlockedChangeEvent is received  mid: " + event.getMediaId() + "  vid: " + event.getVideoId() + "  isBlocked: " + event.isBlocked());
        if (_activeCallLocusKey == null) {
            return;
        }
        CallImpl activeCall = _calls.get(_activeCallLocusKey);
        if (activeCall != null && activeCall.isGroup()) {
            int vid = event.getVideoId();
            switch (event.getMediaId()) {
                case MediaEngine.VIDEO_MID:
                    // If 'blocked' is changed, publish blocked change event.
                    if (vid == 0) {
                        CallObserver observer = activeCall.getObserver();
                        boolean isSending = !event.isBlocked();
                        Ln.d("_isRemoteSendingVideo: " + _isRemoteSendingVideo + "  isSending: " + isSending);
                        if (observer != null && _isRemoteSendingVideo != isSending) {
                            observer.onMediaChanged(new CallObserver.RemoteSendingVideoEvent(activeCall, isSending));
                        }
                        _isRemoteSendingVideo = isSending;
                    } else if (vid > 0) {
                        AuxStreamImpl auxStream = activeCall.getAuxStream(vid);
                        if (auxStream != null && auxStream.isSendingVideo() == event.isBlocked()) {
                            auxStream.setSendingVideo(!event.isBlocked());
                            if (activeCall.getMultiStreamObserver() != null) {
                                activeCall.getMultiStreamObserver().onAuxStreamChanged(new MultiStreamObserver.AuxStreamSendingVideoEvent(activeCall, auxStream));
                            }
                        }
                    }
                    break;
                case MediaEngine.SHARE_MID:
                    // If 'blocked' is changed, publish blocked change event.
                    break;
                default:
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlActiveSpeakerChangedEvent event) {
        Ln.d("CallControlActiveSpeakerChangedEvent is received vid: " + event.getVideoId() + "   participant: " + event.getParticipant());
        doActiveSpeakerChanged(_calls.get(event.getLocusKey()), event.getParticipant(), event.getVideoId());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlMediaVideoMutedEvent event) {
        Ln.d("CallControlMediaVideoMutedEvent is received  participant: " + event.getLocusParticipant().getPerson().getDisplayName() + "  vid: " + event.getVid() + "  mute: " + event.isMuted());
        doActiveSpeakerChanged(_calls.get(event.getLocusKey()), event.getLocusParticipant(), event.getVid());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MediaAvailabilityChangeEvent event) {
        Ln.d("AvailableMediaChangeEvent is received  mid: " + event.getMediaId() + "  count: " + event.getCount());
        if (_activeCallLocusKey != null) {
            CallImpl activeCall = _calls.get(_activeCallLocusKey);
            if (activeCall != null && event.getMediaId() == MediaEngine.VIDEO_MID) {
                _availableMediaCount = event.getCount();
                doParticipantCountChanged(activeCall);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlMediaDecodeSizeChangedEvent event) {
        Ln.d("CallControlMediaDecodeSizeChangedEvent is received  mid: " + event.getMediaId() + "  size: " + event.getSize());
        if (_activeCallLocusKey == null) {
            return;
        }
        CallImpl activeCall = _calls.get(_activeCallLocusKey);
        if (activeCall != null && activeCall.isGroup()) {
            switch (event.getMediaId()) {
                case MediaEngine.VIDEO_MID:
                    AuxStreamImpl auxStream = activeCall.getAuxStream(event.getVideoId());
                    if (auxStream != null) {
                        auxStream.setSize(event.getSize());
                        if (activeCall.getMultiStreamObserver() != null)
                            activeCall.getMultiStreamObserver().onAuxStreamChanged(new MultiStreamObserver.AuxStreamSizeChangedEvent(activeCall, auxStream));
                    }
                    break;
                case MediaEngine.SHARE_MID:
                    break;
                default:
                    break;
            }
        }
    }

    private void doActiveSpeakerChanged(CallImpl call, LocusParticipant participant, long vid) {
        Ln.d("doActiveSpeakerChanged: " + call + "  person: " + participant);
        if (call == null) {
            return;
        }
        for (CallMembership membership : call.getMemberships()) {
            if (membership.getPersonId().equals(Utils.encode(participant.getPerson().getId()))) {
                if (vid == 0) {
                    CallMembership old = call.getActiveSpeaker();
                    if (old == null || !old.getPersonId().equals(membership.getPersonId())) {
                        call.setActiveSpeaker(membership);
                        if (call.getObserver() != null) {
                            call.getObserver().onMediaChanged(new CallObserver.ActiveSpeakerChangedEvent(call, old, call.getActiveSpeaker()));
                        }
                    }
                } else if (vid > 0 && call.isGroup()) {
                    AuxStreamImpl auxStream = call.getAuxStream(vid);
                    if (auxStream != null && (auxStream.getPerson() == null || !auxStream.getPerson().getPersonId().equals(membership.getPersonId()))) {
                        CallMembership old = auxStream.getPerson();
                        auxStream.setPerson(membership);
                        if (call.getMultiStreamObserver() != null) {
                            call.getMultiStreamObserver().onAuxStreamChanged(new MultiStreamObserver.AuxStreamPersonChangedEvent(call, auxStream, old, auxStream.getPerson()));
                        }
                    }
                }
                break;
            }
        }
    }

    private void doParticipantCountChanged(CallImpl call) {
        if (call == null || !call.getKey().equals(_activeCallLocusKey) || !call.isGroup()) {
            return;
        }
        int oldCount = call.getAvailableAuxStreamCount();
        int newCount = Math.min(call.getJoinedMemberships().size() - 2, _availableMediaCount - 1);
        Ln.d("doParticipantCountChanged old: " + oldCount + "  new: " + newCount);
        newCount = Math.min(newCount, MediaEngine.MAX_NUMBER_STREAMS);
        if (newCount >= 0 && oldCount != newCount) {
            call.setAvailableAuxStreamCount(newCount);
            if (call.getMultiStreamObserver() == null) {
                for (int i = oldCount; i > newCount; i--) {
                    call.closeAuxStream();
                }
            } else if (newCount > oldCount) {
                for (int i = oldCount; i < newCount; i++) {
                    View view = call.getMultiStreamObserver().onAuxStreamAvailable();
                    if (view != null) {
                        call.openAuxStream(view);
                    }
                }
            } else {
                for (int i = oldCount; i > newCount; i--) {
                    View view = call.getMultiStreamObserver().onAuxStreamUnavailable();
                    AuxStream auxStream = call.getAuxStream(view);
                    if (auxStream != null) {
                        call.closeAuxStream(auxStream, view);
                    } else {
                        call.closeAuxStream();
                    }
                }
            }
        } else if (_availableMediaCount == 0 && call.getActiveSpeaker() != null) {
            CallMembership old = call.getActiveSpeaker();
            call.setActiveSpeaker(null);
            if (call.getObserver() != null) {
                call.getObserver().onMediaChanged(new CallObserver.ActiveSpeakerChangedEvent(call, old, call.getActiveSpeaker()));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Screen Sharing
    // ------------------------------------------------------------------------
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FloorRequestAcceptedEvent event) {
        Ln.d("FloorRequestAcceptedEvent is received: " + event.isContent());
        if (event.isContent() && event.getLocusKey().equals(_screenSharingKey)) {
            ScreenShareContext.getInstance().init(_context, Activity.RESULT_OK, _screenSharingIntent);
            CallImpl call = _calls.get(_screenSharingKey);
            if (call != null && call.getShareRequestCallback() != null) {
                call.getShareRequestCallback().onComplete(ResultImpl.success(null));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FloorRequestDeniedEvent event) {
        Ln.d("FloorRequestDeniedEvent is received: " + event.isContent());
        if (event.isContent() && event.getLocusKey().equals(_screenSharingKey)) {
            CallImpl call = _calls.get(_screenSharingKey);
            if (call != null && call.getShareRequestCallback() != null) {
                call.getShareRequestCallback().onComplete(ResultImpl.error("Share request is deined"));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FloorReleasedAcceptedEvent event) {
        Ln.d("FloorReleasedAcceptedEvent is received: " + event.isContent());
        if (event.isContent() && event.getLocusKey().equals(_screenSharingKey)) {
            CallImpl call = _calls.get(_screenSharingKey);
            if (call != null && call.getShareReleaseCallback() != null) {
                call.getShareReleaseCallback().onComplete(ResultImpl.success(null));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FloorReleasedDeniedEvent event) {
        Ln.d("FloorReleasedDeniedEvent is received: " + event.isContent());
        if (event.isContent() && event.getLocusKey().equals(_screenSharingKey)) {
            CallImpl call = _calls.get(_screenSharingKey);
            if (call != null && call.getShareReleaseCallback() != null) {
                call.getShareReleaseCallback().onComplete(ResultImpl.error("Share release is deined"));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FloorLostEvent event) {
        Ln.d("FloorLostEvent is received");
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null && call.getStatus() == Call.CallStatus.CONNECTED) {
            LocusParticipant beneficiary = event.getLocalMediaShare().getFloor().getBeneficiary();
            if (beneficiary != null) {
                LocusData locusData = _callControlService.getLocusData(event.getLocusKey());
                if (locusData != null) {
                    _lostSharingParticipant = locusData.getParticipant(beneficiary.getUrl());
                    if (_lostSharingParticipant.getPerson() == null)
                        Ln.w("_lostSharingParticipant getPerson is null");
                    else
                        Ln.d("_lostSharingParticipant email: " + _lostSharingParticipant.getPerson().getEmail());
                }
            }
        }
    }

    public void setScreenshotPermission(final Intent permissionIntent) {
        if (permissionIntent != null) {
            _screenSharingIntent = permissionIntent;
            _callControlService.shareScreen(_screenSharingKey);
        } else {
            Ln.w("permission for sharing screen is deined");
            CallImpl call = _calls.get(_screenSharingKey);
            if (call != null && call.getShareRequestCallback() != null) {
                call.getShareRequestCallback().onComplete(ResultImpl.error(STR_PERMISSION_DENIED));
            }
        }
    }

    public void startSharing(LocusKey key) {
        Ln.d("startSharing: " + key);
        _screenSharingKey = key;
        final Intent intent = new Intent(_context, AcquirePermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquirePermissionActivity.PERMISSION_TYPE, AcquirePermissionActivity.PERMISSION_SCREEN_SHOT);
        _context.startActivity(intent);
    }

    public void stopSharing(LocusKey key) {
        _callControlService.unshareScreen(key);
    }

    protected boolean isSharingFromThisDevice(LocusData locusData) {
        return locusData != null && locusData.isFloorMineThisDevice(_device.getUrl());
    }

    private void removeCall(@NonNull CallObserver.CallDisconnectedEvent event) {
        CallImpl call = (CallImpl) event.getCall();
        Ln.i("Remove " + call);
        if (call != null) {
            call.setStatus(Call.CallStatus.DISCONNECTED);
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onDisconnected(event);
            }
            _calls.remove(call.getKey());
            if (_bus.isRegistered(call)) {
                _bus.unregister(call);
            }
            if (call.getKey().equals(_activeCallLocusKey)) {
                _lostSharingParticipant = null;
                _currentSharingUri = null;
                _availableMediaCount = 0;
            }
        }
    }

    private void setCallOnConnected(@NonNull CallImpl call, @NonNull LocusKey key) {
        if (call.getStatus() == Call.CallStatus.CONNECTED) {
            Ln.d("Already has been connected, return");
            return;
        }
        if (call.getOption() != null) {
            if (call.getOption().hasVideo() && call.getVideoRenderViews() != null) {
                _callControlService.setPreviewWindow(key, call.getVideoRenderViews().first);
                if (call.getVideoRenderViews().first instanceof SurfaceView) {
                    ((SurfaceView) call.getVideoRenderViews().first).getHolder().addCallback(new SurfaceHolder.Callback() {
                        @Override
                        public void surfaceCreated(SurfaceHolder surfaceHolder) {
                            Ln.d("preview surfaceCreated !!!");
                            if (!_callControlService.isPreviewWindowAttached(key, call.getVideoRenderViews().first))
                                _callControlService.setPreviewWindow(key, call.getVideoRenderViews().first);
                        }

                        @Override
                        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                            Ln.d("preview surfaceChanged !!!");
                        }

                        @Override
                        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                            Ln.d("preview surfaceDestroyed !!!");
                            _registerTimer.post(() -> _callControlService.removePreviewWindow(key, call.getVideoRenderViews().first));
                        }
                    });
                }

                _callControlService.setRemoteWindow(key, call.getVideoRenderViews().second);
                if (call.getVideoRenderViews().second instanceof SurfaceView) {
                    ((SurfaceView) call.getVideoRenderViews().second).getHolder().addCallback(new SurfaceHolder.Callback() {
                        @Override
                        public void surfaceCreated(SurfaceHolder surfaceHolder) {
                            Ln.d("remote surfaceCreated !!!");
                            if (!_callControlService.isRemoteWindowAttached(key, 0, call.getVideoRenderViews().second))
                                _callControlService.setRemoteWindow(key, call.getVideoRenderViews().second);
                        }

                        @Override
                        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                            Ln.d("remote surfaceChanged !!!");
                        }

                        @Override
                        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                            Ln.d("remote surfaceDestroyed !!!");
                            _registerTimer.post(() -> _callControlService.removeRemoteWindow(key, call.getVideoRenderViews().second));
                        }
                    });
                }
            }
            if (call.getOption().hasSharing() && call.getSharingRenderView() != null) {
                _callControlService.setShareWindow(key, call.getSharingRenderView());
                if (call.getSharingRenderView() instanceof SurfaceView) {
                    ((SurfaceView) call.getSharingRenderView()).getHolder().addCallback(new SurfaceHolder.Callback() {
                        @Override
                        public void surfaceCreated(SurfaceHolder surfaceHolder) {
                            Ln.d("share surfaceCreated !!!");
                            _registerTimer.postDelayed(() -> _callControlService.setShareWindow(key, call.getSharingRenderView()), 100);
                        }

                        @Override
                        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                            Ln.d("share surfaceChanged !!!");
                        }

                        @Override
                        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                            Ln.d("share surfaceDestroyed !!!");
                            if (_callControlService.isShareRendering()) {
                                _registerTimer.post(() -> _callControlService.removeShareWindow(key));
                            }
                        }
                    });
                }
            }
            call.updateMedia();
        }
        call.setStatus(Call.CallStatus.CONNECTED);

        CallObserver observer = call.getObserver();
        if (observer != null) {
            observer.onConnected(call);
        } else {
            Ln.d("call observer is null");
        }
        _isRemoteSendingVideo = true;
        _isRemoteSendingAudio = call.isRemoteSendingAudio();
        _activeCallLocusKey = key;
    }

    private void setCallOnRinging(@NonNull CallImpl call) {
        if (call.getStatus() == Call.CallStatus.INITIATED) {
            call.setStatus(Call.CallStatus.RINGING);
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onRinging(call);
            }
        } else {
            Ln.w("Do not setCallOnRinging, because current state is: " + call.getStatus());
        }
    }

    private void doCallMembershipChanged(CallImpl call, @NonNull List<CallObserver.CallMembershipChangedEvent> events) {
        if (call == null) {
            return;
        }
        CallObserver observer = call.getObserver();
        if (observer != null) {
            for (CallObserver.CallMembershipChangedEvent event : events) {
                observer.onCallMembershipChanged(event);
            }
        } else {
            Ln.d("call observer is null");
        }
    }

    private void resetDialStatus(Result result) {
        Ln.d("Try to reset dial status for " + result + ", callback " + _dialCallback);
        _dialOption = null;
        if (_dialCallback != null) {
            CompletionHandler<Call> callback = _dialCallback;
            _dialCallback = null;
            if (result != null) {
                callback.onComplete(result);
            }
        }
    }

    private void doDial(String target, MediaOption option) {
        Ln.d("Dial " + target);
        CallContext.Builder builder = new CallContext.Builder(target);
        builder = builder.setMediaDirection(mediaOptionToMediaDirection(option));
        doDial(builder.build());
    }

    private void doDialSpaceID(String target, MediaOption option) {
        Ln.d("Dial " + target);
        apiClientProvider.getConversationClient().getOrCreatePermanentLocus(target).enqueue(new Callback<LocusUrlResponse>() {
            @Override
            public void onResponse(retrofit2.Call<LocusUrlResponse> call, Response<LocusUrlResponse> response) {
                if (response.isSuccessful()) {
                    LocusKey key = LocusKey.fromUri(response.body().getLocusUrl());
                    CallContext.Builder builder = new CallContext.Builder(key);
                    builder = builder.setMediaDirection(mediaOptionToMediaDirection(option));
                    doDial(builder.build());
                } else {
                    Ln.w(STR_FAILURE_CALL + response.errorBody().toString());
                    resetDialStatus(ResultImpl.error(STR_FAILURE_CALL + response.errorBody().toString()));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<LocusUrlResponse> call, Throwable t) {
                resetDialStatus(ResultImpl.error(STR_FAILURE_CALL + t.getMessage()));
            }
        });
    }

    private boolean doDial(CallContext context) {
        MediaCapabilityConfig config = new MediaCapabilityConfig(audioMaxBandwidth, videoMaxBandwidth, sharingMaxBandwidth);
        config.setHardwareCodecEnable(isEnableHardwareAcceleration);
        config.setHwVideoSetting(hardwareVideoSettings);
        _mediaEngine.setMediaConfig(config);
        if (_callControlService.joinCall(context, false) == null) {
            resetDialStatus(ResultImpl.error(STR_FAILURE_CALL + "cannot dial"));
            return false;
        }
        return true;
    }

    private boolean isJoinedFromThisDevice(List<LocusParticipantDevice> devices) {
        if (_device == null || _device.getUrl() == null) {
            Ln.w("isJoinedFromThisDevice: self device is null, register device first.");
            return false;
        }

        for (LocusParticipantDevice device : devices) {
            if (device.getUrl().equals(_device.getUrl())
                    && device.getState() == LocusParticipant.State.JOINED) {
                return true;
            }
        }

        return false;
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

    static MediaEngine.MediaDirection mediaOptionToMediaDirection(MediaOption option) {
        MediaEngine.MediaDirection direction;
        if (option.hasVideo() && option.hasSharing()) {
            direction = MediaEngine.MediaDirection.SendReceiveAudioVideoShare;
        } else if (option.hasVideo() && !option.hasSharing()) {
            direction = MediaEngine.MediaDirection.SendReceiveAudioVideo;
        } else if (!option.hasVideo() && option.hasSharing()) {
            direction = MediaEngine.MediaDirection.SendReceiveShareOnly;
        } else {
            direction = MediaEngine.MediaDirection.SendReceiveAudioOnly;
        }

        return direction;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NoSubscriberEvent event) {
        // -- Ignore Event
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConversationSyncQueue.ConversationSyncStartedEvent event) {
        // -- Ignore Event
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ApplicationControllerStateChangedEvent event) {
        // -- Ignore Event
    }
}
