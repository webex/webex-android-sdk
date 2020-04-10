/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.View;
import com.cisco.wme.appshare.ScreenShareContext;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.crypto.KeyManager;
import com.ciscowebex.androidsdk.internal.mercury.*;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.media.MediaCapability;
import com.ciscowebex.androidsdk.internal.media.WMEngine;
import com.ciscowebex.androidsdk.internal.model.*;
import com.ciscowebex.androidsdk.internal.reachability.BackgroundChecker;
import com.ciscowebex.androidsdk.phone.*;
import com.ciscowebex.androidsdk.utils.Utils;
import com.github.benoitdion.ln.Ln;

import java.util.*;

public class PhoneImpl implements Phone, UIEventHandler.EventObserver, MercuryService.MecuryListener, BackgroundChecker.BackgroundListener {

    enum State {
        REGISTERING, REGISTERED, UNREGISTERING, UNREGISTERED
    }

    private FacingMode facingMode = FacingMode.USER;

    private IncomingCallListener incomingCallListener;

    private final Context context;

    private final Authenticator authenticator;

    private Device device;

    private Credentials credentials;

    private MercuryService mercury;

    private final H264LicensePrompter prompter = new H264LicensePrompter();

    private State state = State.UNREGISTERED;

    private MediaEngine engine;

    private Map<String, CallImpl> calls = new HashMap<>();

    private CallContext callContext;

    private int audioMaxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_AUDIO.getValue();

    private int videoMaxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_720P.getValue();

    private int sharingMaxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_SESSION.getValue();

    private String hardwareVideoSetting = null;

    private boolean hardwareCodecEnable = false;

    private List<String> audioEnhancementModels = null;

    private final CallService service;

    private final ReachabilityService reachability;

    private final MetricsService metrics;

    private MediaSession previewSession;

    private List<ActivityListener> listeners = new ArrayList<>();

    private BackgroundChecker checker;

    public PhoneImpl(Context context, Authenticator authenticator, MediaEngine engine) {
        this.context = context;
        this.authenticator = authenticator;
        this.engine = engine;
        this.service = new CallService(authenticator);
        this.reachability = new ReachabilityService(this);
        this.metrics = new MetricsService(authenticator);
    }

    public void addActivityListener(ActivityListener listener) {
        listeners.add(listener);
    }

    public Context getContext() {
        return this.context;
    }

    public Device getDevice() {
        return device;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public MediaEngine getEngine() {
        return engine;
    }

    public void setChecker(BackgroundChecker checker) {
        this.checker = checker;
    }

    @Override
    public void register(@NonNull CompletionHandler<Void> callback) {
        Queue.main.run(() -> {
            if (state == State.REGISTERING) {
                Ln.w("Already registering");
                callback.onComplete(ResultImpl.error("Already registering"));
                return;
            }
            if (state == State.UNREGISTERING) {
                Ln.w("Already unregistering");
                callback.onComplete(ResultImpl.error("Already unregistering"));
                return;
            }
            state = State.REGISTERING;
            Ln.i("Registering");
            UIEventHandler.get().registerUIEventHandler(context, this);
            Queue.serial.run(new RegisterOperation(authenticator, result -> {
                Pair<Device, Credentials> data = result.getData();
                if (data == null) {
                    Ln.i("Register failed, " + result.getError());
                    state = State.UNREGISTERED;
                    ResultImpl.errorInMain(callback, result);
                    Queue.serial.yield();
                }
                else {
                    device = data.first;
                    credentials = data.second;
                    Ln.i("Registered %s with %s", device.getDeviceUrl(), credentials.getPerson());
                    state = State.REGISTERED;
                    Settings.shared.store(Device.DEVICE_URL, device.getDeviceUrl());
                    mercury = new MercuryService(authenticator, this);
                    mercury.connect(device.getWebSocketUrl(), error -> {
                        if (error != null) {
                            Ln.e("Register failed: " + error);
                        }
                        Queue.serial.underlying().run(() -> {
                            listActiveCalls();
                            Queue.main.run(() -> {
                                reachability.fetch();
                                if (checker != null) {
                                    checker.start();
                                }
                                callback.onComplete(error == null ? ResultImpl.success(null) : ResultImpl.error(error));
                            });
                            Queue.serial.yield();
                        });
                    });
                }
            }));

        });
    }

    @Override
    public void onConnected(@Nullable WebexError error) {
        Ln.d("Mercury connected %s", error);
    }

    @Override
    public void onDisconnected(@Nullable WebexError error) {
        Ln.d("Mercury disconnected %s", error);
    }

    @Override
    public void onEvent(@NonNull MercuryEvent event) {
        if (event instanceof MercuryActivityEvent) {
            ActivityModel model = ((MercuryActivityEvent) event).getActivity();
            if (model != null) {
                for (ActivityListener listener : listeners) {
                    listener.processActivity(model);
                }
            }
        }
        else if (event instanceof MercuryLocusEvent) {
            LocusModel model = ((MercuryLocusEvent) event).getLocus();
            if (model != null) {
                doLocusEvent(model);
            }
        }
        else if (event instanceof MercuryKmsMessageEvent) {
            KmsMessageModel model = ((MercuryKmsMessageEvent) event).getEncryptionKmsMessage();
            if (model != null) {
                KeyManager.shared.processKmsMessage(model);
            }
        }
    }

    @Override
    public void onTransition(boolean foreground) {
        Ln.d("Status transition: " + foreground);
        Queue.serial.run(() -> {
            if (mercury != null && calls.size() == 0) {
                if (foreground) {
                    mercury.tryReconnect();
                } else {
                    mercury.disconnect();
                }
            }
            for (CallImpl call : calls.values()) {
                MediaSession session = call.getMedia();
                if (session != null && session.isRunning()) {
                    if (foreground) {
                        session.prepareToLeaveVideoInterruption();
                    }
                    else {
                        session.prepareToEnterVideoInterruption();
                    }
                }
            }
            Queue.serial.yield();
        });
    }

    @Override
    public void deregister(@NonNull CompletionHandler<Void> callback) {
        Queue.main.run(() -> {
            if (state == State.REGISTERING) {
                Ln.w("Already registering");
                callback.onComplete(ResultImpl.error("Already registering"));
                return;
            }
            if (state == State.UNREGISTERING) {
                Ln.w("Already unregistering");
                callback.onComplete(ResultImpl.error("Already unregistering"));
                return;
            }
            state = State.UNREGISTERING;
            Ln.i("Unregistering");
            UIEventHandler.get().unregisterUIEventHandler(context);
            Queue.serial.run(new UnregisterOperation(authenticator, device, result -> {
                Ln.i("Unregistered");
                mercury.disconnect();
                Queue.main.run(() -> {
                    reachability.clear();
                    if (checker != null) {
                        checker.stop();
                    }
                });
                device = null;
                credentials = null;
                Settings.shared.clear(Device.DEVICE_URL);
                state = State.UNREGISTERED;
                callback.onComplete(result);
                Queue.serial.yield();
            }));
        });
    }

    @Override
    public void onScreenRotation(int rotation) {
        Ln.d("Screen rotation is changed to " + rotation);
        engine.setDisplayRotation(rotation);
    }

    @Override
    public void dial(@NonNull String dialString, @NonNull MediaOption option, @NonNull CompletionHandler<Call> callback) {
        Ln.d("Dialing: " + dialString + ", " + option.hasVideo());
        Queue.serial.run(() -> {
            stopPreview();
            if (callContext != null) {
                Ln.w("Already calling");
                callback.onComplete(ResultImpl.error("Already calling"));
                Queue.serial.yield();
                return;
            }
            if (device == null) {
                Ln.e("Unregistered device");
                callback.onComplete(ResultImpl.error("Unregistered device"));
                Queue.serial.yield();
                return;
            }
            for (CallImpl call : getCalls()) {
                if (!call.isGroup() || (call.isGroup() && call.getStatus() == Call.CallStatus.CONNECTED)) {
                    Ln.e("There are other active calls");
                    callback.onComplete(ResultImpl.error("There are other active calls"));
                    Queue.serial.yield();
                    return;
                }
            }
            callContext = new CallContext.Outgoing(dialString, option, callback);
            Queue.main.run(this::tryAcquirePermission);
            Queue.serial.yield();
        });
    }

    @Override
    public void startPreview(View view) {
        stopPreview();
        Queue.main.run(() -> {
            engine.setDisplayRotation(Utils.getScreenRotation(context));
            previewSession = engine.createPreviveSession(createCapability(), view);
            previewSession.startPreview();
        });
    }

    @Override
    public void stopPreview() {
        Queue.main.run(() -> {
            if (previewSession != null) {
                previewSession.stopPreview();
                previewSession = null;
            }
        });
    }

    @Override
    public void onMediaPermission(boolean permission) {
        Queue.serial.run(() -> {
            String callId = UUID.randomUUID().toString();
            if (callContext instanceof CallContext.Outgoing) {
                CallContext.Outgoing outgoing = (CallContext.Outgoing) callContext;
                callContext = null;
                if (!permission) {
                    Ln.w("permission deined");
                    Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error("permission deined")));
                    Queue.serial.yield();
                    return;
                }
                MediaSession session = engine.createSession(createCapability(), outgoing.getOption());
                String localSdp = session.getLocalSdp();
                MediaEngineReachabilityModel reachabilities = reachability.getFeedback();
                CallService.DialTarget.lookup(outgoing.getTarget(), authenticator, lookupResult -> {
                    CallService.DialTarget target = lookupResult.getData();
                    if (target == null) {
                        Ln.e("Cannot find dial target. " + lookupResult.getError());
                        Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error(lookupResult.getError())));
                        Queue.serial.yield();
                        return;
                    }
                    if (target.isEndpoint()) {
                        service.call(target.getAddress(), callId, device, localSdp, outgoing.getOption().getLayout(), reachabilities, callResult -> {
                            if (callResult.getError() != null || callResult.getData() == null) {
                                Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error(callResult.getError())));
                                Queue.serial.yield();
                                return;
                            }
                            doLocusResponse(new LocusResponse.Call(device, session, callResult.getData(), outgoing.getCallback()), Queue.serial);
                        });
                    }
                    else {
                        service.getOrCreatePermanentLocus(target.getAddress(), device, convResult -> {
                            String url = convResult.getData();
                            if (url == null) {
                                Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error(convResult.getError())));
                                Queue.serial.yield();
                                return;
                            }
                            service.join(url, callId, device, localSdp, outgoing.getOption().getLayout(), reachabilities, joinResult -> {
                                if (joinResult.getError() != null || joinResult.getData() == null) {
                                    Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error(joinResult.getError())));
                                    Queue.serial.yield();
                                    return;
                                }
                                doLocusResponse(new LocusResponse.Call(device, session, joinResult.getData(), outgoing.getCallback()), Queue.serial);
                            });
                        });
                    }
                });
            }
            else if (callContext instanceof CallContext.Incoming) {
                CallContext.Incoming incoming = (CallContext.Incoming) callContext;
                callContext = null;
                if (!permission) {
                    Ln.w("permission deined");
                    Queue.main.run(() -> incoming.getCallback().onComplete(ResultImpl.error("permission deined")));
                    Queue.serial.yield();
                    return;
                }
                MediaSession session = engine.createSession(createCapability(), incoming.getOption());
                incoming.getCall().setMedia(session);
                String localSdp = session.getLocalSdp();
                service.join(incoming.getCall().getUrl(), callId, device, localSdp, incoming.getCall().isGroup() ? incoming.getOption().getLayout() : null, reachability.getFeedback(), joinResult -> {
                    if (joinResult.getError() != null || joinResult.getData() == null) {
                        Queue.main.run(() -> incoming.getCallback().onComplete(ResultImpl.error(joinResult.getError())));
                        Queue.serial.yield();
                        return;
                    }
                    doLocusResponse(new LocusResponse.Answer(incoming.getCall(), joinResult.getData(), incoming.getCallback()), Queue.serial);
                });
            }
            else {
                Ln.e("No call context: " + callContext);
                Queue.serial.yield();
            }
        });
    }

    @Override
    public void onScreenCapturePermission(Intent permission) {
        Ln.d("StartSharing " + callContext);
        Queue.serial.run(() -> {
            if (callContext instanceof CallContext.Sharing) {
                CallImpl call = ((CallContext.Sharing) callContext).getCall();
                CompletionHandler<Void> callback = ((CallContext.Sharing) callContext).getCallback();
                callContext = null;
                if (call.getMedia() == null || !call.getMedia().hasSharing()) {
                    Ln.e("Media option unsupport content share");
                    callback.onComplete(ResultImpl.error("Media option unsupport content share"));
                    Queue.serial.yield();
                    return;
                }
                if (call.isSharingFromThisDevice()) {
                    Ln.e("Already shared by self");
                    callback.onComplete(ResultImpl.error("Already shared by self"));
                    Queue.serial.yield();
                    return;
                }
                if (call.getStatus() != Call.CallStatus.CONNECTED) {
                    Ln.e("No active call");
                    callback.onComplete(ResultImpl.error("No active call"));
                    Queue.serial.yield();
                    return;
                }
                FloorModel floor = new FloorModel(call.getModel().getSelf(), call.getModel().getSelf(), FloorModel.Disposition.GRANTED);
                MediaShareModel mediaShare = new MediaShareModel(MediaShareModel.SHARE_CONTENT_TYPE, call.getModel().getMediaShareUrl(), floor);
                String url = mediaShare.getUrl();
                if (url == null) {
                    Ln.e("Unsupport media share");
                    Queue.serial.yield();
                    return;
                }
                service.update(mediaShare, url, device, result -> {
                    if (result.getError() != null) {
                        callback.onComplete(ResultImpl.error(result.getError()));
                        Queue.serial.yield();
                        return;
                    }
                    doLocusResponse(new LocusResponse.MediaShare(call, FloorModel.Disposition.GRANTED, permission, callback), Queue.serial);
                });
            }
            else {
                Queue.serial.yield();
            }
        });

    }

    @Override
    public IncomingCallListener getIncomingCallListener() {
        return incomingCallListener;
    }

    @Override
    public void setIncomingCallListener(IncomingCallListener listener) {
        this.incomingCallListener = listener;
    }

    @Override
    public FacingMode getDefaultFacingMode() {
        return facingMode;
    }

    @Override
    public void setDefaultFacingMode(FacingMode mode) {
        facingMode = mode;
    }

    @Override
    public void requestVideoCodecActivation(@NonNull AlertDialog.Builder builder, @NonNull CompletionHandler<Boolean> callback) {
        prompter.check(builder, result -> callback.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void disableVideoCodecActivation() {
        prompter.setVideoLicenseActivationDisabled(true);
    }

    @Override
    public String getVideoCodecLicense() {
        return prompter.getLicense();
    }

    @Override
    public String getVideoCodecLicenseURL() {
        return prompter.getLicenseURL();
    }

    @Override
    public void setAudioMaxBandwidth(int bandwidth) {
        audioMaxBandwidth = bandwidth;
    }

    @Override
    public int getAudioMaxBandwidth() {
        return audioMaxBandwidth;
    }

    @Override
    public void setVideoMaxBandwidth(int bandwidth) {
        videoMaxBandwidth = bandwidth;
    }

    @Override
    public int getVideoMaxBandwidth() {
        return videoMaxBandwidth;
    }

    @Override
    public void setSharingMaxBandwidth(int bandwidth) {
        sharingMaxBandwidth = bandwidth;
    }

    @Override
    public int getSharingMaxBandwidth() {
        return sharingMaxBandwidth;
    }

    @Override
    public boolean isHardwareAccelerationEnabled() {
        return hardwareCodecEnable;
    }

    @Override
    public void setHardwareAccelerationEnabled(boolean enable) {
       hardwareCodecEnable = true;
    }

    public String getHardwareVideoSettings() {
        return hardwareVideoSetting;
    }

    public void setHardwareVideoSettings(String settings) {
        hardwareVideoSetting = settings;
    }

    @Override
    public void enableAudioEnhancementForModels(List<String> models) {
        audioEnhancementModels = models;
    }

    void acknowledge(CallImpl call, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            for (CallImpl already : getCalls()) {
                if (!call.getUrl().equals(already.getUrl())) {
                    Ln.e("There are other active calls");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("There are other active calls")));
                    Queue.serial.yield();
                    return;
                }
            }
            if (call.getDirection() == Call.Direction.OUTGOING) {
                Ln.e("Unsupport function for outgoing call");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Unsupport function for outgoing call")));
                Queue.serial.yield();
                return;
            }
            if (call.getDirection() == Call.Direction.INCOMING && call.getStatus() != Call.CallStatus.INITIATED) {
                Ln.e("Not initialted call");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Not initialted call")));
                Queue.serial.yield();
                return;
            }
            String url = call.getUrl();
            if (url == null) {
                Ln.e("Missing call URL");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing call URL")));
                Queue.serial.yield();
                return;
            }
            service.alert(url, device, result -> {
                if (result.getError() != null) {
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                    Queue.serial.yield();
                    return;
                }
                doLocusResponse(new LocusResponse.Ack(call, callback), Queue.serial);
            });
        });
    }

    void answer(CallImpl call, MediaOption option, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            for (CallImpl already : getCalls()) {
                if (already.getUrl().equals(call.getUrl()) && already.getStatus() == Call.CallStatus.CONNECTED) {
                    Ln.e("There are other active calls");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("There are other active calls")));
                    Queue.serial.yield();
                    return;
                }
            }
            if (call.getDirection() == Call.Direction.OUTGOING) {
                Ln.e("Unsupport function for outgoing call");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Unsupport function for outgoing call")));
                Queue.serial.yield();
                return;
            }
            if (call.getDirection() == Call.Direction.INCOMING) {
                if (call.getStatus() == Call.CallStatus.CONNECTED) {
                    Ln.e("Already connected");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already connected")));
                    Queue.serial.yield();
                    return;
                }
                else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                    Ln.e("Already disconnected");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already disconnected")));
                    Queue.serial.yield();
                    return;
                }
            }
            callContext = new CallContext.Incoming(call, option, callback);
            Queue.main.run(this::tryAcquirePermission);
            Queue.serial.yield();
        });
    }

    void reject(CallImpl call, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            if (call.getDirection() == Call.Direction.OUTGOING) {
                Ln.e("Unsupport function for outgoing call");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Unsupport function for outgoing call")));
                Queue.serial.yield();
                return;
            }
            if (call.getDirection() == Call.Direction.INCOMING) {
                if (call.getStatus() == Call.CallStatus.CONNECTED) {
                    Ln.e("Already connected");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already connected")));
                    Queue.serial.yield();
                    return;
                }
                else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                    Ln.e("Already disconnected");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already disconnected")));
                    Queue.serial.yield();
                    return;
                }
            }
            String url = call.getUrl();
            if (url == null) {
                Ln.e("Missing call URL");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing call URL")));
                Queue.serial.yield();
                return;
            }
            service.decline(url, device, result -> {
                if (result.getError() != null) {
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                    Queue.serial.yield();
                    return;
                }
                doLocusResponse(new LocusResponse.Reject(call, callback), Queue.serial);
            });
        });
    }

    void hangup(CallImpl call, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                Ln.e("Already disconnected");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already disconnected")));
                Queue.serial.yield();
                return;
            }
            String url = call.getModel().getSelf().getUrl();
            if (url == null) {
                Ln.e("Missing self participant URL");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing self participant URL")));
                Queue.serial.yield();
                return;
            }
            Queue.main.run(() -> {
                if (call.isSendingSharing()) {
                    stopSharing(call, result -> Ln.d("Stop sharing when call is endded."));
                    call.getMedia().leaveSharing(true);
                }
                service.leave(url, device, result -> {
                    if (result.getError() != null) {
                        Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                        Queue.serial.yield();
                        return;
                    }
                    doLocusResponse(new LocusResponse.Leave(call, result.getData(), callback), Queue.serial);
                });
            });

        });
    }

    void update(CallImpl call, boolean audio, boolean video, String localSdp, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            String url = call.getModel().getSelf().getMediaBaseUrl();
            String sdp = call.getModel().getLocalSdp();
            if (sdp == null) {
                sdp = localSdp;
            }
            String mediaId = call.getModel().getMediaId(device.getDeviceUrl());
            if (url == null || sdp == null || mediaId == null) {
                Ln.e("Missing media data");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing media data")));
                Queue.serial.yield();
                return;
            }
            MediaInfoModel media = new MediaInfoModel(localSdp == null ? sdp : localSdp, !audio, !video, reachability.getFeedback().reachability);
            service.update(url, mediaId, device, media, result -> {
                if (result.getError() != null && result.getData() == null) {
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                    Queue.serial.yield();
                    return;
                }
                doLocusResponse(new LocusResponse.Update(call, result.getData(), callback), Queue.serial);
            });
        });
    }

    void fetch(CallImpl call, boolean full) {
        Queue.serial.run(() -> {
            String sync = null;
            LocusSequenceModel sequence = call.getModel().getSequence();
            if (!full && (sequence == null || sequence.isEmpty())) {
                sync = call.getModel().getSyncUrl();
                Ln.d("Requesting delta sync for locus: " + sync);
            }
            if (sync == null) {
                sync = call.getUrl();
                Ln.d("Requesting full sync for locus: " + sync);
            }
            service.get(sync, result -> {
                if (result.getError() != null) {
                    Queue.serial.yield();
                    return;
                }
                doLocusResponse(new LocusResponse.Update(call, result.getData(), null), Queue.serial);
            });
        });
    }

    void dtmf(CallImpl call, String tones, int correlationId, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            if (call.isSendingDTMFEnabled() && call.getModel().getSelf() != null) {
                String url = call.getModel().getSelf().getUrl();
                service.dtmf(url, device, correlationId, tones, null, null, result -> {
                    Queue.main.run(() -> callback.onComplete(result));
                    Queue.serial.yield();
                });
            }
            else {
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("unsupported DTMF")));
                Queue.serial.yield();
            }
        });

    }

    void admit(CallImpl call, List<CallMembership> memberships, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            String url = call.getUrl();
            if (url == null) {
                Ln.e("Missing call URL");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing call URL")));
                Queue.serial.yield();
                return;
            }
            service.admit(url, memberships, result -> {
                if (result.getError() != null) {
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                    Queue.serial.yield();
                    return;
                }
                doLocusResponse(new LocusResponse.Update(call, result.getData(), callback), Queue.serial);
            });
        });
    }

    void feedback(Map<String, String> feedback) {
        feedback.put("key", "meetup_call_user_rating");
        feedback.put("time", String.valueOf(System.currentTimeMillis()));
        feedback.put("type", "GENERIC");
        List<Map<String, String>> list = new ArrayList<>();
        list.add(feedback);
        metrics.post(list);
    }

    void keepAlive(CallImpl call) {
        String url = call.getModel().getKeepAliveUrl(device.getDeviceUrl());
        if (url == null) {
            Ln.e("Missing keepAlive URL");
            return;
        }
        service.keepalive(url, result -> {});
    }

    void startSharing(CallImpl call, CompletionHandler<Void> callback) {
        callContext = new CallContext.Sharing(call, callback);
        final Intent intent = new Intent(context, AcquirePermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquirePermissionActivity.PERMISSION_TYPE, AcquirePermissionActivity.PERMISSION_SCREEN_SHOT);
        context.startActivity(intent);
    }

    void stopSharing(CallImpl call, CompletionHandler<Void> callback) {
        Ln.d("StopSharing " + call.getUrl());
        Queue.serial.run(() -> {
            if (call.getMedia() == null || !call.getMedia().hasSharing()) {
                Ln.e("Media option unsupport content share");
                callback.onComplete(ResultImpl.error("Media option unsupport content share"));
                Queue.serial.yield();
                return;
            }
            if (!call.isSharingFromThisDevice()) {
                Ln.e("Local share screen not start");
                callback.onComplete(ResultImpl.error("Local share screen not start"));
                Queue.serial.yield();
                return;
            }
            FloorModel floor = new FloorModel(call.getModel().getSelf(), call.getModel().getSelf(), FloorModel.Disposition.RELEASED);
            MediaShareModel mediaShare = new MediaShareModel(MediaShareModel.SHARE_CONTENT_TYPE, call.getModel().getMediaShareUrl(), floor);
            String url = mediaShare.getUrl();
            if (url == null) {
                Ln.e("Unsupport media share");
                Queue.serial.yield();
                return;
            }
            service.update(mediaShare, url, device, result -> {
                if (result.getError() != null) {
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                    Queue.serial.yield();
                    return;
                }
                doLocusResponse(new LocusResponse.MediaShare(call, FloorModel.Disposition.RELEASED, null, callback), Queue.serial);
            });
        });
    }

    Collection<CallImpl> getCalls() {
        return Collections.unmodifiableCollection(calls.values());
    }

    private void tryAcquirePermission() {
        final Intent intent = new Intent(context, AcquirePermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquirePermissionActivity.PERMISSION_TYPE, AcquirePermissionActivity.PERMISSION_CAMERA_MIC);
        context.startActivity(intent);
    }

    private void doLocusResponse(LocusResponse response, Queue queue) {
        Ln.d("doLocusResponse: " + response);
        if (response instanceof LocusResponse.Call) {
            LocusModel model = ((LocusResponse.Call) response).getLocus();
            Ln.d("Connecting call: " + model.getCallUrl());
            CallImpl call = new CallImpl(model, this, device, ((LocusResponse.Call) response).getSession(), Call.Direction.OUTGOING, !model.isOneOnOne());
            if (call.isStatusIllegal()) {
                Ln.d("The previous session did not end");
                Queue.main.run(() -> {
                    WebexError error = new WebexError(WebexError.ErrorCode.UNEXPECTED_ERROR, "The previous session did not end");
                    ((LocusResponse.Call) response).getCallback().onComplete(ResultImpl.error(error));
                    call.end(new CallObserver.CallErrorEvent(call, error));
                    queue.yield();
                });
                return;
            }
            addCall(call);
            Queue.main.run(() -> {
                if (call.getModel().isSelfInLobby()) {
                    call.startKeepAlive();
                }
                else {
                    call.startMedia();
                }
                ((LocusResponse.Call) response).getCallback().onComplete(ResultImpl.success(call));
                queue.yield();
            });
        }
        else if (response instanceof LocusResponse.Answer) {
            LocusResponse.Answer res = (LocusResponse.Answer) response;
            res.getCall().update(res.getResult());
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        }
        else if (response instanceof LocusResponse.Leave) {
            LocusResponse.Leave res = (LocusResponse.Leave) response;
            LocusModel model = res.getResult();
            if (model != null) {
                res.getCall().update(model);
            }
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        }
        else if (response instanceof LocusResponse.Reject) {
            LocusResponse.Reject res = (LocusResponse.Reject) response;
            res.getCall().end(new CallObserver.LocalDecline(res.getCall()));
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        }
        else if (response instanceof LocusResponse.Ack) {
            LocusResponse.Ack res = (LocusResponse.Ack) response;
            res.getCall().setStatus(Call.CallStatus.RINGING);
            Queue.main.run(() -> {
                CallObserver observer = res.getCall().getObserver();
                if (observer != null) {
                    res.getCall().getObserver().onRinging(res.getCall());
                }
                if (res.getCallback() != null) {
                    res.getCallback().onComplete(ResultImpl.success(null));
                }
                queue.yield();
            });
        }
        else if (response instanceof LocusResponse.Update) {
            LocusResponse.Update res = (LocusResponse.Update) response;
            LocusModel model = res.getResult();
            if (model != null) {
                res.getCall().update(model);
            }
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        }
        else if (response instanceof LocusResponse.MediaShare) {
            LocusResponse.MediaShare res = (LocusResponse.MediaShare) response;
            Queue.main.run(() -> {
                if (res.getIntent() != null && FloorModel.Disposition.GRANTED == res.getDisposition()) {
                    ScreenShareContext.getInstance().init(context, Activity.RESULT_OK, res.getIntent());
                }
                if (res.getCallback() != null) {
                    res.getCallback().onComplete(ResultImpl.success(null));
                }
                queue.yield();
            });
        }
    }

    private void doLocusEvent(LocusModel model) {
        if (model == null) {
            Ln.d("No CallModel");
            return;
        }
        String url = model.getCallUrl();
        if (url == null) {
            Ln.d("CallModel is missing call url");
            return;
        }
        Ln.d("doLocusEvent: " + url);
        Queue.serial.run(() -> {
            CallImpl call = calls.get(url);
            if (call == null) {
                if (device != null && model.isIncomingCall() && model.isValid()) {
                    CallImpl incoming = new CallImpl(model, this, device, null, Call.Direction.INCOMING, !model.isOneOnOne());
                    addCall(incoming);
                    Ln.d("Receive incoming call: " + incoming.getUrl());
                    Queue.main.run(() -> {
                        IncomingCallListener listener = getIncomingCallListener();
                        if (listener != null) {
                            listener.onIncomingCall(incoming);
                        }
                    });
                }
                else {
                    Ln.d("Receive incoming call with invalid model");
                }
                Queue.serial.yield();
                return;
            }
            call.update(model);
            Queue.serial.yield();
        });

    }

    void addCall(CallImpl call) {
        Ln.d("Add call for call url: " + call.getUrl());
        this.calls.put(call.getUrl(), call);
    }

    void removeCall(CallImpl call) {
        Ln.d("Remove call for call url: " + call.getUrl());
        this.calls.remove(call.getUrl());
    }

    CallService getService() {
        return service;
    }

    void listActiveCalls() {
        Ln.d("Fetch call infos");
        if (device != null) {
            service.list(device, result -> {
                List<LocusModel> models = result.getData();
                if (models == null) {
                    Ln.d("Failure: " + result.getError());
                    return;
                }
                for (LocusModel model : models) {
                    doLocusEvent(model);
                }
                Ln.d("Success: fetch call infos");
            });
        }
    }

    private MediaCapability createCapability() {
        MediaCapability capability = new MediaCapability();
        capability.setAudioMaxBandwidth(getAudioMaxBandwidth());
        capability.setVideoMaxBandwidth(getVideoMaxBandwidth());
        capability.setSharingMaxBandwidth(getSharingMaxBandwidth());
        capability.setHardwareCodecEnable(isHardwareAccelerationEnabled());
        capability.setHardwareVideoSetting(getHardwareVideoSettings());
        capability.setAudioEnhancementModels(audioEnhancementModels);
        if (device != null) {
            capability.setDeviceSettings(device.getDeviceSettings());
        }
        capability.setDefaultCamera(WMEngine.Camera.fromFaceMode(getDefaultFacingMode()));
        return capability;
    }
}
