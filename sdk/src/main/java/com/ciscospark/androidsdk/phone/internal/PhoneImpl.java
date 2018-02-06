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
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.View;

import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.callcontrol.CallContext;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.callcontrol.events.CallControlCallCancelledEvent;
import com.cisco.spark.android.callcontrol.events.CallControlCallJoinErrorEvent;
import com.cisco.spark.android.callcontrol.events.CallControlFloorGrantedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlFloorReleasedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLeaveLocusEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalAudioMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocalVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusChangedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlLocusCreatedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantAudioMuteEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantJoinedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.CallControlRemoteVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlSelfParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.DismissCallNotificationEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.events.ApplicationControllerStateChangedEvent;
import com.cisco.spark.android.events.CallNotificationEvent;
import com.cisco.spark.android.events.CallNotificationType;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.events.RequestCallingPermissions;
import com.cisco.spark.android.locus.events.LocusDeclinedEvent;
import com.cisco.spark.android.locus.events.ParticipantNotifiedEvent;
import com.cisco.spark.android.locus.events.RetrofitErrorEvent;
import com.cisco.spark.android.locus.events.ParticipantRoomDeclinedEvent;
import com.cisco.spark.android.locus.events.ParticipantSelfChangedEvent;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipantDevice;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.responses.LocusUrlResponse;
import com.cisco.spark.android.media.MediaCapabilityConfig;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSession;
import com.cisco.spark.android.media.events.StunTraceServerResultEvent;
import com.cisco.spark.android.metrics.CallAnalyzerReporter;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.Spark;
import com.ciscospark.androidsdk.SparkError;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.internal.MetricsClient;
import com.ciscospark.androidsdk.internal.ResultImpl;
import com.ciscospark.androidsdk.internal.SparkInjector;
import com.ciscospark.androidsdk.people.Person;
import com.ciscospark.androidsdk.people.internal.PersonClientImpl;
import com.ciscospark.androidsdk.phone.Call;
import com.ciscospark.androidsdk.phone.CallMembership;
import com.ciscospark.androidsdk.phone.CallObserver;
import com.ciscospark.androidsdk.phone.MediaOption;
import com.ciscospark.androidsdk.phone.Phone;
import com.ciscospark.androidsdk.utils.Utils;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
import com.github.benoitdion.ln.Ln;

import me.helloworld.utils.Checker;
import retrofit2.Callback;
import retrofit2.Response;

import com.cisco.spark.android.locus.model.LocusParticipant;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.NoSubscriberEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PhoneImpl implements Phone {

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

    @Inject
    CallAnalyzerReporter _callAnalyzerReporter;

    @Inject
    transient Settings settings;

    private IncomingCallListener _incomingCallListener;

    private Authenticator _authenticator;

    private DeviceRegistration _device;

    private Handler _registerTimer;

    private Runnable _registerTimeoutTask;

    private CompletionHandler<Void> _registerCallback;

    private Map<LocusKey, CallImpl> _calls = new HashMap<>();

    private CompletionHandler<Call> _dialCallback;

    private MediaOption _dialOption;

    private MediaSession _preview;

    private H264LicensePrompter _prompter;

    private MetricsClient _metrics;

    private Context _context;

    private int audioMaxBandwidth = DefaultBandwidth.maxBandwidthAudio.getValue();

    private int videoMaxBandwidth = DefaultBandwidth.maxBandwidth720p.getValue();

    private int shareMaxBandwidth = DefaultBandwidth.maxBandwidthSession.getValue();

    private static class DialTarget {
        private String address;
        private AddressType type;

        enum AddressType {
            PEOPLE_ID,
            PEOPLE_MAIL,
            ROOM_ID,
            ROOM_MAIL,
            OTHER
        }

        public boolean isEndpoint() {
            switch (this.type) {
                case OTHER:
                case PEOPLE_ID:
                case ROOM_MAIL:
                case PEOPLE_MAIL:
                    return true;
                case ROOM_ID:
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
                case ROOM_MAIL:
                case ROOM_ID:
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
                return;
            } else if (this.address.toLowerCase().endsWith("@meet.ciscospark.com")) {
                this.type = AddressType.ROOM_MAIL;
                return;
            } else if (this.address.contains("@") && !this.address.contains(".")) {
                this.type = AddressType.PEOPLE_MAIL;
                return;
            } else {
                this.type = AddressType.OTHER;
                return;
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
                        String type = paths.get(paths.size() - 2);
                        if (type.equalsIgnoreCase("PEOPLE")) {
                            this.type = AddressType.PEOPLE_ID;
                            return paths.get(paths.size() - 1);
                        } else if (type.equalsIgnoreCase("ROOM")) {
                            this.type = AddressType.ROOM_ID;
                            return paths.get(paths.size() - 1);
                        }
                    }
                }
                return null;
            } catch (Throwable t) {
                return null;
            }

        }

    }

    public PhoneImpl(Context context, Authenticator authenticator, SparkInjector injector) {
        injector.inject(this);
        _authenticator = authenticator;
        _bus.register(this);
        _registerTimer = new Handler();
        _context = context;
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
            callback.onComplete(ResultImpl.success(result));
        });
    }

    public String getVideoCodecLicense() {
        return _prompter.getLicense();
    }

    public String getVideoCodecLicenseURL() {
        return _prompter.getLicenseURL();
    }

    @Override
    public void setAudioMaxBandwidth(int bandwidth) {
        audioMaxBandwidth = bandwidth <= 0 ? DefaultBandwidth.maxBandwidthAudio.getValue() : bandwidth;
    }

    @Override
    public int getAudioMaxBandwidth() {
        return audioMaxBandwidth;
    }

    @Override
    public void setVideoMaxBandwidth(int bandwidth) {
        videoMaxBandwidth = bandwidth <= 0 ? DefaultBandwidth.maxBandwidth720p.getValue() : bandwidth;
    }

    @Override
    public int getVideoMaxBandwidth() {
        return videoMaxBandwidth;
    }

    @Override
    public void setShareMaxBandwidth(int bandwidth) {
        shareMaxBandwidth = bandwidth <= 0 ? DefaultBandwidth.maxBandwidthSession.getValue() : bandwidth;
    }

    @Override
    public int getShareMaxBandwidth() {
        return shareMaxBandwidth;
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

        ServiceBuilder.async(_authenticator, callback, s -> {
            _registerTimeoutTask = () -> {
                Ln.i("Register timeout");
                if (_device == null && _registerCallback != null) {
                    _registerCallback.onComplete(ResultImpl.error("Register timeout"));
                }
            };
            _registerTimer.postDelayed(_registerTimeoutTask, 60 * 1000);
            new AuthenticatedUserTask(_applicationController).execute();
        });
    }

    public void deregister(@NonNull CompletionHandler<Void> callback) {
        Ln.i("Deregistering");
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
                Ln.e("There are other active calls");
                callback.onComplete(ResultImpl.error("There are other active calls"));
                return;
            }
        }
        stopPreview();
        _dialOption = option;
        _dialCallback = callback;

        DialTarget target = new DialTarget(dialString);

        if (target.type == DialTarget.AddressType.PEOPLE_MAIL) {
            new PersonClientImpl(_authenticator).list(dialString, null, 1, new CompletionHandler<List<Person>>() {
                @Override
                public void onComplete(Result<List<Person>> result) {
                    List<Person> persons = result.getData();
                    if (!Checker.isEmpty(persons)) {
                        Person person = persons.get(0);
                        Ln.d("Lookup target: " + person.getId());
                        doDial(new DialTarget(person.getId()).address, option);
                    } else {
                        doDial(target.address, option);
                    }
                }
            });
        } else if (target.isEndpoint()) {
            doDial(target.address, option);
        } else {
            doDialRoomID(target.address, option);
        }
    }

    void answer(@NonNull CallImpl call) {
        Ln.i("Answer " + call);
        for (CallImpl exist : _calls.values()) {
            Ln.d("answer exist.getStatus(): " + exist.getStatus());
            if (!exist.getKey().equals(call.getKey()) && (exist.getStatus() == Call.CallStatus.RINGING || exist.getStatus() == Call.CallStatus.CONNECTED)) {
                Ln.e("There are other active calls");
                if (call.getAnswerCallback() != null) {
                    call.getAnswerCallback().onComplete(ResultImpl.error("There are other active calls"));
                }
                return;
            }
        }
        if (call.getDirection() == Call.Direction.OUTGOING) {
            Ln.e("Unsupport function for outgoing call");
            if (call.getAnswerCallback() != null) {
                call.getAnswerCallback().onComplete(ResultImpl.error("Unsupport function for outgoing call"));
            }
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
                Ln.e("Already connected");
                if (call.getAnswerCallback() != null) {
                    call.getAnswerCallback().onComplete(ResultImpl.error("Already connected"));
                }
                return;
            } else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                Ln.e("Already disconnected");
                if (call.getAnswerCallback() != null) {
                    call.getAnswerCallback().onComplete(ResultImpl.error("Already disconnected"));
                }
                return;
            }
        }
        stopPreview();
        CallContext.Builder builder = new CallContext.Builder(call.getKey()).setIsAnsweringCall(true).setIsOneOnOne(!call.isGroup());
        builder = builder.setMediaDirection(mediaOptionToMediaDirection(call.getOption()));
        _mediaEngine.setMediaConfig(new MediaCapabilityConfig(audioMaxBandwidth, videoMaxBandwidth, shareMaxBandwidth));
        _callControlService.joinCall(builder.build(), false);
    }

    void reject(@NonNull CallImpl call) {
        Ln.i("Reject " + call);
        if (call.getDirection() == Call.Direction.OUTGOING) {
            Ln.e("Unsupport function for outgoing call");
            if (call.getRejectCallback() != null) {
                call.getRejectCallback().onComplete(ResultImpl.error("Unsupport function for outgoing call"));
            }
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
                Ln.e("Already connected");
                if (call.getRejectCallback() != null) {
                    call.getRejectCallback().onComplete(ResultImpl.error("Already connected"));
                }
                return;
            } else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                Ln.e("Already disconnected");
                if (call.getRejectCallback() != null) {
                    call.getRejectCallback().onComplete(ResultImpl.error("Already disconnected"));
                }
                return;
            }
        }
        _callControlService.declineCall(call.getKey());
    }

    void hangup(@NonNull CallImpl call) {
        Ln.i("Hangup " + call);
        if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
            Ln.e("Already disconnected");
            if (call.getHangupCallback() != null) {
                call.getHangupCallback().onComplete(ResultImpl.error("Already disconnected"));
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

    OperationQueue getOperationQueue() {
        return _operationQueue;
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
            if (locus != null) {
                call = new CallImpl(this, _dialOption, CallImpl.Direction.OUTGOING, key, locus.getLocusData().isMeeting());
                _bus.register(call);
                _calls.put(key, call);
                if (_dialCallback != null) {
                    _dialCallback.onComplete(ResultImpl.success(call));
                }
                if (call.isGroup()) {
                    _setCallOnRinging(call);
                    _setCallOnConnected(call, event.getLocusKey());
                }
            } else {
                Ln.e("Internal callImpl isn't exist " + event.getLocusKey());
                if (_dialCallback != null) {
                    _dialCallback.onComplete(ResultImpl.error("Internal callImpl isn't exist"));
                }
                _dialOption = null;
            }
            _dialCallback = null;
        }
    }

    // Remoted send acknowledge and it means it is RINGING
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ParticipantNotifiedEvent event) {
        Ln.i("ParticipantNotifiedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d("Find callImpl " + event.getLocusKey());
            if (call.isGroup()) {
                if (call.getDirection() == Call.Direction.INCOMING) {
                    _setCallOnRinging(call);
                }
            }else{
                if (call.getDirection() == Call.Direction.INCOMING) {
                    for (LocusParticipant participant : call.getRemoteParticipants()) {
                        Ln.d("participant State: " + participant.getState());
                        if (participant.getState() == LocusParticipant.State.JOINED) {
                            _setCallOnRinging(call);
                            break;
                        }
                    }
                }else{
                    _setCallOnRinging(call);
                }
            }
        }
    }

    // Remote accept call, call will be setup
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlParticipantJoinedEvent event) {
        Ln.i("CallControlParticipantJoinedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d("Find callImpl " + event.getLocusKey());
            if (!call.isGroup()) {
                if (call.getAnswerCallback() != null && (call.getStatus() == Call.CallStatus.INITIATED || call.getStatus() == Call.CallStatus.RINGING)) {
                    call.getAnswerCallback().onComplete(ResultImpl.success(null));
                }
                _setCallOnConnected(call, event.getLocusKey());
            } else if (call.getStatus() != Call.CallStatus.CONNECTED) {
                for (LocusParticipant locusParticipant : event.getJoinedParticipants()) {
                    if (locusParticipant.getDeviceUrl().equals(_device.getUrl())) {
                        if (call.getAnswerCallback() != null) {
                            call.getAnswerCallback().onComplete(ResultImpl.success(null));
                        }
                        if (_dialCallback != null) {
                            _dialCallback.onComplete(ResultImpl.success(call));
                            _setCallOnRinging(call);
                            _dialCallback = null;
                        }
                        if (call.getOption() == null && _dialOption != null) {
                            call.setMediaOption(_dialOption);
                        }
                        _setCallOnConnected(call, event.getLocusKey());
                    }
                }
            }
        } else {
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
                    }
                    _setCallOnRinging(call);
                    _setCallOnConnected(call, event.getLocusKey());
                    _dialCallback = null;
                }
            }
        }
        //call membership changed
        List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
        for (LocusParticipant locusParticipant : event.getJoinedParticipants()) {
            events.add(new CallObserver.MembershipJoinedEvent(call, new CallMembershipImpl(locusParticipant, call)));
        }
        _sendCallMembershipChanged(call, events);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RetrofitErrorEvent event) {
        Ln.e("RetrofitErrorEvent is received ");
        clearCallback(ResultImpl.error("Error"));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlCallJoinErrorEvent event) {
        Ln.e("CallControlCallJoinErrorEvent is received ");
        clearCallback(ResultImpl.error("Join Error"));
    }

    // Local hangup
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlSelfParticipantLeftEvent event) {
        Ln.i("CallControlSelfParticipantLeftEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d("Find callImpl " + event.getLocusKey());
            if (call.getHangupCallback() != null) {
                call.getHangupCallback().onComplete(ResultImpl.success(null));
            }
            _removeCall(new CallObserver.LocalLeft(call));
        }
    }

    // Local declined
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LocusDeclinedEvent event) {
        Ln.i("LocusDeclinedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            if (call.getRejectCallback() != null) {
                call.getRejectCallback().onComplete(ResultImpl.success(null));
            }
            Ln.d("Find callImpl " + event.getLocusKey());
            _removeCall(new CallObserver.LocalDecline(call));
        }
    }

    // Local & Remote cancel
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlCallCancelledEvent event) {
        Ln.i("CallControlCallCancelledEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d("Find callImpl " + event.getLocusKey());
            if (event.getReason() == CallControlService.CancelReason.REMOTE_CANCELLED) {
                _removeCall(new CallObserver.RemoteCancel(call));
            } else {
                if (call.getHangupCallback() != null) {
                    call.getHangupCallback().onComplete(ResultImpl.success(null));
                }
                _removeCall(new CallObserver.LocalCancel(call));
            }
        }
    }

    // Remote hangup
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlParticipantLeftEvent event) {
        Ln.i("CallControlParticipantLeftEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d("Find callImpl " + event.getLocusKey());
            if (!call.isGroup()) {
                _removeCall(new CallObserver.RemoteLeft(call));
            } else if (call.getStatus() == Call.CallStatus.INITIATED || call.getStatus() == Call.CallStatus.RINGING) {
                boolean meetingIsOpen = false;
                for (CallMembership membership : call.getMemberships()) {
                    if (membership.getState() == CallMembership.State.JOINED) {
                        meetingIsOpen = true;
                    }
                }
                if (!meetingIsOpen) {
                    _removeCall(new CallObserver.RemoteCancel(call));
                }
            }
            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            for (LocusParticipant locusParticipant : event.getLeftParticipants()) {
                events.add(new CallObserver.MembershipLeftEvent(call, new CallMembershipImpl(locusParticipant, call)));
            }
            _sendCallMembershipChanged(call, events);
        }
    }

    // Remote declined
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlLeaveLocusEvent event) {
        Ln.i("CallControlLeaveLocusEvent is received " + event.locusData().getKey());
        CallImpl call = _calls.get(event.locusData().getKey());
        if (call != null && (!(event.wasMediaFlowing() || event.wasUCCall() || event.wasRoomCall() || event.wasRoomCallConnected()))) {
            Ln.d("Find callImpl " + event.locusData().getKey());
            _removeCall(new CallObserver.RemoteDecline(call));
        }
    }

    // Room remote declined
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ParticipantRoomDeclinedEvent event) {
        Ln.i("ParticipantRoomDeclinedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d("Find group call " + event.getLocusKey());
            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            for (LocusParticipant locusParticipant : event.getDeclinedParticipants()) {
                events.add(new CallObserver.MembershipDeclinedEvent(call, new CallMembershipImpl(locusParticipant, call)));
            }
            _sendCallMembershipChanged(call, events);
        }

    }

    // Incoming Call
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallNotificationEvent event) {
        Ln.i("CallNotificationEvent is received " + event.getType());
        if (event.getType() == CallNotificationType.INCOMING) {
            Ln.i("InComing Call " + event.getLocusKey());
            LocusData locusData = _callControlService.getLocusData(event.getLocusKey());
            boolean isGroup = locusData != null ? locusData.isMeeting() : false;
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
        if (call != null && event.getLocus() != null
                && event.getLocus().getSelf().getDevices() != null) {
            List deviceList = event.getLocus().getSelf().getDevices();
            if (call.getStatus() == Call.CallStatus.CONNECTED && !isJoinedFromThisDevice(deviceList)) {
                Ln.d("Local device left locusKey: " + event.getLocusKey());
                if (call.getHangupCallback() != null) {
                    call.getHangupCallback().onComplete(ResultImpl.success(null));
                }
                _removeCall(new CallObserver.LocalLeft(call));
            } else if (call.getStatus() != Call.CallStatus.CONNECTED
                    && isJoinedFromOtherDevice(deviceList)
                    && !isJoinedFromThisDevice(deviceList)) {
                com.cisco.spark.android.callcontrol.model.Call locus = _callControlService.getCall(event.getLocusKey());
                if (locus == null || !locus.isActive()) {
                    Ln.d("other device connected locusKey: " + event.getLocusKey());
                    _removeCall(new CallObserver.OtherConnected(call));
                }else{
                    Ln.d("Self device has already connected, ignore other device connect");
                }
            } else if (call.getStatus() != Call.CallStatus.CONNECTED
                    && !isJoinedFromThisDevice(deviceList)
                    && event.getLocus().getSelf().getState() == LocusParticipant.State.DECLINED) {
                com.cisco.spark.android.callcontrol.model.Call locus = _callControlService.getCall(event.getLocusKey());
                if (locus == null || !locus.isActive()) {
                    Ln.d("other device declined locusKey: " + event.getLocusKey());
                    _removeCall(new CallObserver.OtherDeclined(call));
                }else{
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
            Ln.d("Find callImpl " + event.getLocusKey());
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
            Ln.d("Find callImpl " + event.getLocusKey());
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
            Ln.d("Find callImpl " + event.getLocusKey());
            LocusSelfRepresentation self = call.getSelf();
            if (self == null || !self.getUrl().equals(event.getParticipant().getUrl())) {
                CallObserver observer = call.getObserver();
                if (observer != null) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingAudioEvent(call, !event.isMuted()));
                }
            } else {
                // TODO for local ??
            }
            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            events.add(new CallObserver.MembershipSendingAudioEvent(call, new CallMembershipImpl(event.getParticipant(), call)));
            _sendCallMembershipChanged(call, events);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlRemoteVideoMutedEvent event) {
        Ln.i("CallControlRemoteVideoMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            Ln.d("Find callImpl " + event.getLocusKey());
            LocusSelfRepresentation self = call.getSelf();
            if (self == null || !self.getUrl().equals(event.getParticipant().getUrl())) {
                CallObserver observer = call.getObserver();
                if (observer != null) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingVideoEvent(call, !event.isMuted()));
                }
            } else {
                // TODO for local ??
            }
            List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
            events.add(new CallObserver.MembershipSendingVideoEvent(call, new CallMembershipImpl(event.getParticipant(), call)));
            _sendCallMembershipChanged(call, events);
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
        clearCallback(ResultImpl.error(new SparkError<>(SparkError.ErrorCode.PERMISSION_ERROR, "Permissions Error", permissions)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlFloorGrantedEvent event) {
        Ln.i("CallControlFloorGrantedEvent is received ");
        List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
            for (CallMembership membership : call.getMemberships()) {
                if (membership.isSendingShare()) {
                    events.add(new CallObserver.MembershipSendingSharingEvent(call, membership));
                }
            }
            _sendCallMembershipChanged(call, events);

            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onMediaChanged(new CallObserver.RemoteSendingShareEvent(call, true));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlFloorReleasedEvent event) {
        Ln.i("CallControlFloorReleasedEvent is received ");
        List<CallObserver.CallMembershipChangedEvent> events = new ArrayList<>();
        CallImpl call = _calls.get(event.getLocusKey());
        LocusData locusData = _callControlService.getLocusData(event.getLocusKey());
        if (call != null && locusData != null
                && locusData.getLocus().isFloorReleased()
                && locusData.getReleasedParticipantSharing() != null) {
            LocusParticipant beneficiary = locusData.getReleasedParticipantSharing();
            for (CallMembership membership : call.getMemberships()) {
                if (beneficiary.getPerson() != null
                        && beneficiary.getPerson().getId() != null
                        && membership.getPersonId().equalsIgnoreCase(beneficiary.getPerson().getId())) {
                    events.add(new CallObserver.MembershipSendingSharingEvent(call, membership));
                }
            }
            _sendCallMembershipChanged(call, events);

            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onMediaChanged(new CallObserver.RemoteSendingShareEvent(call, false));
            }
        }
    }


    private void _removeCall(@NonNull CallObserver.CallDisconnectedEvent event) {
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
        }
    }

    private void _setCallOnConnected(@NonNull CallImpl call, @NonNull LocusKey key) {
        if (call.getStatus() == Call.CallStatus.CONNECTED) {
            Ln.d("Already has been connected, return");
            return;
        }
        if (call.getOption() != null) {
            if (call.getOption().hasVideo() && call.getVideoRenderViews() != null) {
                _callControlService.setRemoteWindow(key, call.getVideoRenderViews().second);
                _callControlService.setPreviewWindow(key, call.getVideoRenderViews().first);
            }
            if (call.getOption().hasShare() && call.getShareRenderView() != null) {
                _callControlService.setShareWindow(key, call.getShareRenderView());
            }
            _callControlService.updateMediaSession(_callControlService.getCall(call.getKey()), mediaOptionToMediaDirection(call.getOption()));
        }
        call.setStatus(Call.CallStatus.CONNECTED);

        CallObserver observer = call.getObserver();
        if (observer != null) {
            observer.onConnected(call);
        } else {
            Ln.d("call observer is null");
        }
    }

    private void _setCallOnRinging(@NonNull CallImpl call) {
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

    private void _sendCallMembershipChanged(CallImpl call, @NonNull List<CallObserver.CallMembershipChangedEvent> events) {
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

    private void clearCallback(Result result) {
        _dialOption = null;
        if (_dialCallback != null) {
            _dialCallback.onComplete(result);
            _dialCallback = null;
        }
    }

    private void doDial(String target, MediaOption option) {
        Ln.d("Dial " + target);
        CallContext.Builder builder = new CallContext.Builder(target);
        builder = builder.setMediaDirection(mediaOptionToMediaDirection(option));
        _mediaEngine.setMediaConfig(new MediaCapabilityConfig(audioMaxBandwidth, videoMaxBandwidth, shareMaxBandwidth));
        _callControlService.joinCall(builder.build(), false);
    }

    private void doDialRoomID(String target, MediaOption option) {
        Ln.d("Dial " + target);
        apiClientProvider.getConversationClient().getOrCreatePermanentLocus(target).enqueue(new Callback<LocusUrlResponse>() {
            @Override
            public void onResponse(retrofit2.Call<LocusUrlResponse> call, Response<LocusUrlResponse> response) {
                if (response.isSuccessful()) {
                    LocusKey key = LocusKey.fromUri(response.body().getLocusUrl());
                    CallContext.Builder builder = new CallContext.Builder(key);
                    builder = builder.setMediaDirection(mediaOptionToMediaDirection(option));
                    _mediaEngine.setMediaConfig(new MediaCapabilityConfig(audioMaxBandwidth, videoMaxBandwidth, shareMaxBandwidth));
                    _callControlService.joinCall(builder.build(), false);
                } else {
                    Ln.w("Failure call: " + response.errorBody().toString());
                    clearCallback(ResultImpl.error("Failure call: " + response.errorBody().toString()));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<LocusUrlResponse> call, Throwable t) {
                clearCallback(ResultImpl.error("Failure call: " + t.getMessage()));
            }
        });
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

    private boolean isJoinedFromOtherDevice(List<LocusParticipantDevice> devices) {
        if (_device == null || _device.getUrl() == null) {
            Ln.w("isJoinedFromOtherDevice: self device is null, register device first.");
            return false;
        }

        for (LocusParticipantDevice device : devices) {
            if (!device.getUrl().equals(_device.getUrl())
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
        MediaEngine.MediaDirection direction = MediaEngine.MediaDirection.SendReceiveAudioVideoShare;
        if (option.hasVideo() && option.hasShare()) {
            direction = MediaEngine.MediaDirection.SendReceiveAudioVideoShare;
        } else if (option.hasVideo() && !option.hasShare()) {
            direction = MediaEngine.MediaDirection.SendReceiveAudioVideo;
        } else if (!option.hasVideo() && option.hasShare()) {
            direction = MediaEngine.MediaDirection.SendReceiveShareOnly;
        } else {
            direction = MediaEngine.MediaDirection.SendReceiveAudioOnly;
        }

        return direction;
    }

    // -- Ignore Event
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NoSubscriberEvent event) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConversationSyncQueue.ConversationSyncStartedEvent event) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StunTraceServerResultEvent event) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ApplicationControllerStateChangedEvent event) {

    }
}
