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

import java.io.UnsupportedEncodingException;
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
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
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
import com.ciscospark.androidsdk.phone.CallObserver;
import com.ciscospark.androidsdk.phone.MediaOption;
import com.ciscospark.androidsdk.phone.Phone;
import com.ciscospark.androidsdk.utils.Utils;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;
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

    private CompletionHandler<Void> _incomingCallback;

    private MediaOption _option;

    private MediaSession _preview;
    
    private H264LicensePrompter _prompter;
	
	private MetricsClient _metrics;

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
            callback.onComplete(ResultImpl.success(result));
        });
    }
    
    public String getVideoCodecLicense() {
	    return _prompter.getLicense();
    }
    
    public String getVideoCodecLicenseURL() {
	    return _prompter.getLicenseURL();
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
        if (_calls.size() > 0) {
	        Ln.e("There are other active calls");
            callback.onComplete(ResultImpl.error("There are other active calls"));
            return;
        }
        stopPreview();
        _option = option;
        _dialCallback = callback;
	    
	    if (dialString.contains("@") && !dialString.contains(".")) {
		    new PersonClientImpl(_authenticator).list(dialString, null, 1, new CompletionHandler<List<Person>>() {
			    @Override
			    public void onComplete(Result<List<Person>> result) {
				    List<Person> persons = result.getData();
				    if (!Checker.isEmpty(persons)) {
					    Person person = persons.get(0);
					    Ln.d("Lookup target: " + person.getId());
					    doDial(parseHydraId(person.getId()), option);
				    }
				    else {
					    doDial(parseHydraId(dialString), option);
				    }
			    }
		    });
	    }
	    else {
		    doDial(parseHydraId(dialString), option);
	    }
    }
	
    void answer(@NonNull CallImpl call, @NonNull MediaOption option, @NonNull CompletionHandler<Void> callback) {
	    Ln.i("Answer " + call);
        for (CallImpl exist : _calls.values()) {
            Ln.d("answer exist.getStatus(): " + exist.getStatus());
            if (!exist.getKey().equals(call.getKey()) && (exist.getStatus() == Call.CallStatus.RINGING || exist.getStatus() == Call.CallStatus.CONNECTED)) {
	            Ln.e("There are other active calls");
                callback.onComplete(ResultImpl.error("There are other active calls"));
                return;
            }
        }
        if (call.getDirection() == Call.Direction.OUTGOING) {
	        Ln.e("Unsupport function for outgoing call");
            callback.onComplete(ResultImpl.error("Unsupport function for outgoing call"));
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
	            Ln.e("Already connected");
                callback.onComplete(ResultImpl.error("Already connected"));
                return;
            }
            else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
	            Ln.e("Already disconnected");
                callback.onComplete(ResultImpl.error("Already disconnected"));
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
        //_callControlService.joinCall(builder.build());
        _callControlService.joinCall(builder.build(), false);
    }

    void reject(@NonNull CallImpl call, @NonNull CompletionHandler<Void> callback) {
	    Ln.i("Reject " + call);
        if (call.getDirection() == Call.Direction.OUTGOING) {
	        Ln.e("Unsupport function for outgoing call");
            callback.onComplete(ResultImpl.error("Unsupport function for outgoing call"));
            return;
        }
        if (call.getDirection() == Call.Direction.INCOMING) {
            if (call.getStatus() == Call.CallStatus.CONNECTED) {
	            Ln.e("Already connected");
                callback.onComplete(ResultImpl.error("Already connected"));
                return;
            }
            else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
	            Ln.e("Already disconnected");
                callback.onComplete(ResultImpl.error("Already disconnected"));
                return;
            }
        }
        _incomingCallback = callback;
        _callControlService.declineCall(call.getKey());
    }

    void hangup(@NonNull CallImpl call, @NonNull CompletionHandler<Void> callback) {
	    Ln.i("Hangup " + call);
        if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
	        Ln.e("Already disconnected");
            callback.onComplete(ResultImpl.error("Already disconnected"));
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
                call = new CallImpl(this, _option, CallImpl.Direction.OUTGOING, key);
                _bus.register(call);
                _calls.put(key, call);
                if (_dialCallback != null) {
                    _dialCallback.onComplete(ResultImpl.success(call));
                }
            }
            else {
	            Ln.e("Internal callImpl isn't exist " + event.getLocusKey());
                if (_dialCallback != null) {
                    _dialCallback.onComplete(ResultImpl.error("Internal callImpl isn't exist"));
                }
                _option = null;
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
            call.setStatus(Call.CallStatus.RINGING);
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onRinging(call);
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
	        if (call.getStatus() == Call.CallStatus.CONNECTED){
	            Ln.d("Already has been connected, return");
	            return;
            }
	        if (_option != null && _option.hasVideo()) {
		        _callControlService.setRemoteWindow(event.getLocusKey(), _option.getRemoteView());
		        _callControlService.setPreviewWindow(event.getLocusKey(), _option.getLocalView());
	        }
            call.setStatus(Call.CallStatus.CONNECTED);
            if (_incomingCallback != null) {
                _incomingCallback.onComplete(ResultImpl.success(null));
                _incomingCallback = null;
            }
            CallObserver observer = call.getObserver();
            if (observer != null) {
                observer.onConnected(call);
            }
        }
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
            if (_incomingCallback != null) {
                _incomingCallback.onComplete(ResultImpl.success(null));
                _incomingCallback = null;
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
            if (_incomingCallback != null) {
                _incomingCallback.onComplete(ResultImpl.success(null));
                _incomingCallback = null;
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
            }
            else {
                if (_incomingCallback != null) {
                    _incomingCallback.onComplete(ResultImpl.success(null));
                    _incomingCallback = null;
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
            _removeCall(new CallObserver.RemoteLeft(call));
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

    // Incoming Call
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallNotificationEvent event) {
	    Ln.i("CallNotificationEvent is received " + event.getType());
        if (event.getType() == CallNotificationType.INCOMING) {
	        Ln.i("InComing Call " + event.getLocusKey());
            CallImpl call = new CallImpl(this, _option, CallImpl.Direction.INCOMING, event.getLocusKey());
            _bus.register(call);
            _calls.put(call.getKey(), call);
            IncomingCallListener listener = getIncomingCallListener();
            if (listener != null) {
                listener.onIncomingCall(call);
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
            }
            else {
                // TODO for local ??
            }
        }
    }

	@Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CallControlParticipantVideoMutedEvent event) {
	    Ln.i("CallControlParticipantVideoMutedEvent is received " + event.getLocusKey());
        CallImpl call = _calls.get(event.getLocusKey());
        if (call != null) {
	        Ln.d("Find callImpl " + event.getLocusKey());
            LocusSelfRepresentation self = call.getSelf();
            if (self == null || !self.getUrl().equals(event.getParticipant().getUrl())) {
                CallObserver observer = call.getObserver();
                if (observer != null) {
                    observer.onMediaChanged(new CallObserver.RemoteSendingVideoEvent(call, !event.isMuted()));
                }
            }
            else {
                // TODO for local ??
            }
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

	private void doDial(String target, MediaOption option) {
		Ln.d("Dial " + target);
		CallContext.Builder builder = new CallContext.Builder(target);
		if (!option.hasVideo()) {
			builder = builder.setMediaDirection(MediaEngine.MediaDirection.SendReceiveAudioOnly);
		}
		//_callControlService.joinCall(builder.build());
		_callControlService.joinCall(builder.build(), false);
	}
	
    private String parseHydraId(String id) {
		try {
			byte[] bytes = Base64.decode(id, Base64.URL_SAFE);
			if (Checker.isEmpty(bytes)) {
				return id;
			}
			String decode = new String(bytes, "UTF-8");
			Uri uri = Uri.parse(decode);
			if (uri != null && uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("ciscospark")) {
				List<String> paths = uri.getPathSegments();
				if (paths != null && paths.size() >= 2) {
					return paths.get(paths.size() - 1);
				}
			}
			return id;
		}
		catch (UnsupportedEncodingException e) {
			return id;
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

