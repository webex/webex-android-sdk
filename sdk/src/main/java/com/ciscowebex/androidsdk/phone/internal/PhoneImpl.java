/*
 * Copyright 2016-2021 Cisco Systems Inc
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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.View;

import com.cisco.wme.appshare.ScreenShareContext;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.WebexEvent;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.AcquirePermissionActivity;
import com.ciscowebex.androidsdk.internal.Credentials;
import com.ciscowebex.androidsdk.internal.Device;
import com.ciscowebex.androidsdk.internal.MetricsService;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.crypto.KeyManager;
import com.ciscowebex.androidsdk.internal.media.MediaCapability;
import com.ciscowebex.androidsdk.internal.media.WMEngine;
import com.ciscowebex.androidsdk.internal.mercury.MercuryActivityEvent;
import com.ciscowebex.androidsdk.internal.mercury.MercuryEvent;
import com.ciscowebex.androidsdk.internal.mercury.MercuryKmsMessageEvent;
import com.ciscowebex.androidsdk.internal.mercury.MercuryLocusEvent;
import com.ciscowebex.androidsdk.internal.mercury.MercuryService;
import com.ciscowebex.androidsdk.internal.metric.CallAnalyzerReporter;
import com.ciscowebex.androidsdk.internal.model.ActivityModel;
import com.ciscowebex.androidsdk.internal.model.FloorModel;
import com.ciscowebex.androidsdk.internal.model.KmsMessageModel;
import com.ciscowebex.androidsdk.internal.model.LocusModel;
import com.ciscowebex.androidsdk.internal.model.LocusSequenceModel;
import com.ciscowebex.androidsdk.internal.model.MediaEngineReachabilityModel;
import com.ciscowebex.androidsdk.internal.model.MediaInfoModel;
import com.ciscowebex.androidsdk.internal.model.MediaShareModel;
import com.ciscowebex.androidsdk.internal.model.ObjectModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.reachability.BackgroundChecker;
import com.ciscowebex.androidsdk.membership.MembershipObserver;
import com.ciscowebex.androidsdk.membership.internal.InternalMembership;
import com.ciscowebex.androidsdk.message.MessageObserver;
import com.ciscowebex.androidsdk.message.internal.InternalMessage;
import com.ciscowebex.androidsdk.message.internal.MessageClientImpl;
import com.ciscowebex.androidsdk.phone.AdvancedSetting;
import com.ciscowebex.androidsdk.phone.Call;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.CallObserver;
import com.ciscowebex.androidsdk.phone.MediaOption;
import com.ciscowebex.androidsdk.phone.Phone;
import com.ciscowebex.androidsdk.space.SpaceObserver;
import com.ciscowebex.androidsdk.space.internal.InternalSpace;
import com.ciscowebex.androidsdk.utils.Utils;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhoneImpl implements Phone, UIEventHandler.EventObserver, MercuryService.MercuryListener, BackgroundChecker.BackgroundListener {

    enum State {
        REGISTERING, REGISTERED, UNREGISTERING, UNREGISTERED
    }

    private FacingMode facingMode = FacingMode.USER;
    private IncomingCallListener incomingCallListener;
    private final Webex webex;
    private final Context context;
    private final Authenticator authenticator;
    private Device device;
    private Credentials credentials;
    private MercuryService mercury;
    private final H264LicensePrompter prompter = new H264LicensePrompter();
    private State state = State.UNREGISTERED;
    private MediaEngine engine;
    private Map<String, CallImpl> calls = new HashMap<>();
    private List<String> activeSpaceIds = new ArrayList<>();
    private CallContext callContext;
    private final CallService service;
    private final ReachabilityService reachability;
    private final MetricsService metrics;
    private MediaSession previewSession;
    private BackgroundChecker checker;

    private int audioMaxRxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_AUDIO.getValue();
    private int videoMaxRxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_720P.getValue();
    private int videoMaxTxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_720P.getValue();
    private int sharingMaxRxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_SESSION.getValue();
    private String hardwareVideoSetting = null;
    private boolean hardwareCodecEnable = false;
    private List<String> audioEnhancementModels = null;
    private Map<Class<? extends AdvancedSetting>, AdvancedSetting> settings = new HashMap<>();
    private boolean enableBackgroundStream = false;
    private boolean enableAudioBNR = false;
    private AudioBRNMode audioBRNMode = AudioBRNMode.HP;

    private String uuid = UUID.randomUUID().toString();
    private boolean canceled = false;
    private MembershipObserver membershipObserver;
    private SpaceObserver spaceObserver;
    private MessageObserver messageObserver;

    public PhoneImpl(Context context, Webex webex, Authenticator authenticator, MediaEngine engine) {
        this.webex = webex;
        this.context = context;
        this.authenticator = authenticator;
        this.engine = engine;
        this.service = new CallService(authenticator);
        this.reachability = new ReachabilityService(this);
        this.metrics = new MetricsService(authenticator);
        CallAnalyzerReporter.shared.init(this);
    }

    public void setMembershipObserver(MembershipObserver membershipObserver) {
        this.membershipObserver = membershipObserver;
    }

    public void setSpaceObserver(SpaceObserver spaceObserver) {
        this.spaceObserver = spaceObserver;
    }

    public void setMessageObserver(MessageObserver messageObserver) {
        this.messageObserver = messageObserver;
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

    public ReachabilityService getReachability() {
        return reachability;
    }

    public void setChecker(BackgroundChecker checker) {
        this.checker = checker;
    }

    public String getPhoneId() {
        return this.uuid;
    }

    @Override
    public void register(@NonNull CompletionHandler<Void> callback) {
        Queue.main.run(() -> {
            if (state != State.UNREGISTERED) {
                Ln.w("Device is not unregistered");
                callback.onComplete(ResultImpl.error("Device is not unregistered"));
                return;
            }
            state = State.REGISTERING;
            Ln.i("Registering");
            UIEventHandler.get().registerUIEventHandler(context, this);
            onScreenRotation(Utils.getScreenRotation(context));
            Queue.serial.run(new RegisterOperation(authenticator, result -> {
                Pair<Device, Credentials> data = result.getData();
                if (data == null) {
                    Ln.i("Register failed, " + result.getError());
                    state = State.UNREGISTERED;
                    ResultImpl.errorInMain(callback, result);
                    Queue.serial.yield();
                } else {
                    device = data.first;
                    credentials = data.second;
                    Ln.i("Registered %s with %s", device.getDeviceUrl(), credentials.getPerson());
                    state = State.REGISTERED;
                    device.store();
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
            doActivityEvent(((MercuryActivityEvent) event).getActivity());
        } else if (event instanceof MercuryLocusEvent) {
            doLocusEvent(((MercuryLocusEvent) event).getLocus());
        } else if (event instanceof MercuryKmsMessageEvent) {
            KmsMessageModel model = ((MercuryKmsMessageEvent) event).getEncryptionKmsMessage();
            if (model != null) {
                KeyManager.shared.processKmsMessage(model);
            }
        }
    }

    @Override
    public void onTransition(boolean foreground) {
        Ln.d("Status transition: " + foreground + " enableBackgroundStream: " + enableBackgroundStream);
        Queue.serial.run(() -> {
            if (mercury != null) {
                if (foreground) {
                    mercury.tryReconnect();
                } else if (calls.size() == 0) {
                    mercury.disconnect(false);
                }
            }
            for (CallImpl call : calls.values()) {
                MediaSession session = call.getMedia();
                if (session != null && session.isRunning()) {
                    if (foreground) {
                        session.prepareToLeaveVideoInterruption();
                    } else if (!enableBackgroundStream) {
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
            if (state != State.REGISTERED) {
                Ln.w("Device is not registered");
                callback.onComplete(ResultImpl.error("Device is not registered"));
                return;
            }
            state = State.UNREGISTERING;
            Ln.i("Unregistering");
            UIEventHandler.get().unregisterUIEventHandler(context);
            Queue.serial.run(new UnregisterOperation(authenticator, device, result -> {
                Ln.i("Unregistered");
                Queue.main.run(() -> {
                    reachability.clear();
                    if (checker != null) {
                        checker.stop();
                    }
                });
                mercury.disconnect(true);
                device.clear();
                device = null;
                credentials = null;
                state = State.UNREGISTERED;
                if (result.getError() != null && result.getError().is(WebexError.ErrorCode.NETWORK_ERROR)) {
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                } else {
                    Queue.main.run(() -> callback.onComplete(result));
                }
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
        prompter.check(getContext(), null, result -> {
            H264LicenseAction action = result.getData();
            if (action == null || action == H264LicenseAction.ACCEPT) {
                this.canceled = false;
                Queue.serial.run(() -> {
                    stopPreview();
                    if (callContext != null) {
                        Ln.w("Already calling: " + callContext);
                        Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already calling")));
                        Queue.serial.yield();
                        return;
                    }
                    if (device == null) {
                        Ln.e("Unregistered device");
                        Queue.main.run(() -> callback.onComplete(ResultImpl.error("Unregistered device")));
                        Queue.serial.yield();
                        return;
                    }
                    for (CallImpl call : getCalls()) {
                        if (call.getStatus() == Call.CallStatus.CONNECTED) {
                            Ln.e("There are other active calls: " + call);
                            Queue.main.run(() -> callback.onComplete(ResultImpl.error("There are other active calls")));
                            Queue.serial.yield();
                            return;
                        }
                    }
                    callContext = new CallContext.Outgoing(dialString, option, callback);
                    Ln.d("CallContext: " + callContext);
                    Queue.main.run(this::tryAcquirePermission);
                    Queue.serial.yield();
                });
            } else if (action == H264LicenseAction.DECLINE) {
                Ln.d("Decline H264 license");
                ResultImpl.errorInMain(callback, WebexError.from(WebexError.ErrorCode.DECLINE_H264_LICENSE));
            } else if (action == H264LicenseAction.VIEW_LICENSE) {
                Ln.d("View H264 license");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(H264LicensePrompter.LICENSE_URL));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.getContext().startActivity(browserIntent);
                ResultImpl.errorInMain(callback, WebexError.from(WebexError.ErrorCode.VIEW_H264_LICENSE));
            }
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
        Ln.d("CallContext: " + callContext);
        Queue.serial.run(() -> {
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
                CallService.DialTarget.lookup(outgoing.getTarget(), authenticator, target -> {
                    if (this.canceled) {
                        this.canceled = false;
                        Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error("The call be canceled by user")));
                        Queue.serial.yield();
                        return;
                    }
                    String correlationId = UUID.randomUUID().toString();
                    //CallAnalyzerReporter.shared.reportJoinRequest(correlationId, null);
                    if (target instanceof CallService.CallableTarget) {
                        service.call(((CallService.CallableTarget) target).getCallee(), outgoing.getOption(), correlationId, device, localSdp, reachabilities, callResult -> {
                            if (callResult.getError() != null || callResult.getData() == null) {
                                Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error(callResult.getError())));
                                Queue.serial.yield();
                                CallAnalyzerReporter.shared.reportJoinResponseError(correlationId, null, String.valueOf(callResult.getError()));
                                return;
                            }
                            doLocusResponse(new LocusResponse.Call(device, correlationId, session, callResult.getData(), outgoing.getCallback()), Queue.serial);
                        });
                    } else if (target instanceof CallService.JoinableTarget) {
                        service.getOrCreatePermanentLocus(((CallService.JoinableTarget) target).getConversation(), device, convResult -> {
                            if (this.canceled) {
                                this.canceled = false;
                                Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error("The call be canceled by user")));
                                Queue.serial.yield();
                                return;
                            }
                            String url = convResult.getData();
                            if (url == null) {
                                Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error(convResult.getError())));
                                Queue.serial.yield();
                                return;
                            }
                            service.join(url, outgoing.getOption(), correlationId, device, localSdp, reachabilities, joinResult -> {
                                if (joinResult.getError() != null || joinResult.getData() == null) {
                                    Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error(joinResult.getError())));
                                    Queue.serial.yield();
                                    CallAnalyzerReporter.shared.reportJoinResponseError(correlationId, null, String.valueOf(joinResult.getError()));
                                    return;
                                }
                                doLocusResponse(new LocusResponse.Call(device, correlationId, session, joinResult.getData(), outgoing.getCallback()), Queue.serial);
                            });
                        });
                    } else {
                        Ln.e("Cannot find dial target: " + outgoing.getTarget());
                        Queue.main.run(() -> outgoing.getCallback().onComplete(ResultImpl.error("Cannot find dial target: " + outgoing.getTarget())));
                        Queue.serial.yield();
                    }
                });
            } else if (callContext instanceof CallContext.Incoming) {
                CallContext.Incoming incoming = (CallContext.Incoming) callContext;
                callContext = null;
                if (!permission) {
                    Ln.w("permission deined");
                    Queue.main.run(() -> incoming.getCallback().onComplete(ResultImpl.error("permission deined")));
                    Queue.serial.yield();
                    return;
                }
                MediaSession session = engine.createSession(createCapability(), incoming.getOption());
                String localSdp = session.getLocalSdp();
                incoming.getCall().setMedia(session);
                //CallAnalyzerReporter.shared.reportJoinRequest(incoming.getCall().getCorrelationId(), incoming.getCall().getModel().getKey());
                service.join(incoming.getCall().getUrl(), incoming.getOption(), incoming.getCall().getCorrelationId(), device, localSdp, reachability.getFeedback(), joinResult -> {
                    if (joinResult.getError() != null || joinResult.getData() == null) {
                        Queue.main.run(() -> incoming.getCallback().onComplete(ResultImpl.error(joinResult.getError())));
                        Queue.serial.yield();
                        CallAnalyzerReporter.shared.reportJoinResponseError(incoming.getCall().getCorrelationId(), incoming.getCall().getModel().getKey(), String.valueOf(joinResult.getError()));
                        return;
                    }
                    doLocusResponse(new LocusResponse.Answer(incoming.getCall(), joinResult.getData(), incoming.getCallback()), Queue.serial);
                });
            } else {
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
                if (permission == null) {
                    Ln.e("User canceled");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("User canceled")));
                    Queue.serial.yield();
                    return;
                }
                if (call.getMedia() == null || !call.getMedia().hasSharing()) {
                    Ln.e("Media option unsupport content share");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("Media option unsupport content share")));
                    Queue.serial.yield();
                    return;
                }
                if (call.isSharingFromThisDevice()) {
                    Ln.e("Already shared by self");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already shared by self")));
                    Queue.serial.yield();
                    return;
                }
                if (call.getStatus() != Call.CallStatus.CONNECTED) {
                    Ln.e("No active call");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("No active call")));
                    Queue.serial.yield();
                    return;
                }
                FloorModel floor = new FloorModel(call.getModel().getSelf(), call.getModel().getSelf(), FloorModel.Disposition.GRANTED);
                MediaShareModel mediaShare = new MediaShareModel(MediaShareModel.SHARE_CONTENT_TYPE, call.getModel().getMediaShareUrl(), floor);
                String url = mediaShare.getUrl();
                if (url == null) {
                    Ln.e("Unsupport media share");
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("No MediaShare url")));
                    Queue.serial.yield();
                    return;
                }
                service.update(mediaShare, url, device, result -> {
                    if (result.getError() != null) {
                        Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                        Queue.serial.yield();
                        return;
                    }
                    doLocusResponse(new LocusResponse.MediaShare(call, FloorModel.Disposition.GRANTED, permission, callback), Queue.serial);
                });
            } else {
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
    public void cancel() {
        this.canceled = true;
    }

    @Override
    public void enableAudioBNR(boolean enable) {
        this.enableAudioBNR = enable;
    }

    @Override
    public boolean isAudioBNREnable() {
        return enableAudioBNR;
    }

    @Override
    public void setAudioBNRMode(AudioBRNMode mode) {
        this.audioBRNMode = mode;
    }

    @Override
    public AudioBRNMode getAudioBNRMode() {
        return audioBRNMode;
    }

    @Override
    public void requestVideoCodecActivation(@NonNull AlertDialog.Builder builder, @Nullable CompletionHandler<H264LicenseAction> callback) {
        this.prompter.check(getContext(), builder, result -> {
            if (callback == null) {
                if (result.getData() == H264LicenseAction.VIEW_LICENSE) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(H264LicensePrompter.LICENSE_URL));
                    this.getContext().startActivity(browserIntent);
                }
            } else {
                callback.onComplete(result);
            }
        });
    }

    @Override
    public void disableVideoCodecActivation() {
        prompter.setVideoLicenseActivationDisabled(true);
    }

    @Override
    public String getVideoCodecLicense() {
        return H264LicensePrompter.LICENSE_TEXT;
    }

    @Override
    public String getVideoCodecLicenseURL() {
        return H264LicensePrompter.LICENSE_URL;
    }

    @Override
    public void setAudioMaxRxBandwidth(int bandwidth) {
        audioMaxRxBandwidth = bandwidth;
    }

    @Override
    public int getAudioMaxRxBandwidth() {
        return audioMaxRxBandwidth;
    }

    @Override
    public void setVideoMaxRxBandwidth(int bandwidth) {
        videoMaxRxBandwidth = bandwidth;
    }

    @Override
    public int getVideoMaxRxBandwidth() {
        return videoMaxRxBandwidth;
    }

    @Override
    public void setVideoMaxTxBandwidth(int bandwidth) {
        videoMaxTxBandwidth = bandwidth;
    }

    @Override
    public int getVideoMaxTxBandwidth() {
        return videoMaxTxBandwidth;
    }

    @Override
    public void setSharingMaxRxBandwidth(int bandwidth) {
        sharingMaxRxBandwidth = bandwidth;
    }

    @Override
    public int getSharingMaxRxBandwidth() {
        return sharingMaxRxBandwidth;
    }

    @Override
    public void setAudioMaxBandwidth(int bandwidth) {
        setAudioMaxRxBandwidth(bandwidth);
    }

    @Override
    public int getAudioMaxBandwidth() {
        return getAudioMaxRxBandwidth();
    }

    @Override
    public void setVideoMaxBandwidth(int bandwidth) {
        setVideoMaxRxBandwidth(bandwidth);
    }

    @Override
    public int getVideoMaxBandwidth() {
        return getVideoMaxRxBandwidth();
    }

    @Override
    public void setSharingMaxBandwidth(int bandwidth) {
        setSharingMaxRxBandwidth(bandwidth);
    }

    @Override
    public int getSharingMaxBandwidth() {
        return getSharingMaxRxBandwidth();
    }

    @Override
    public void enableBackgroundStream(boolean enable) {
        Ln.d("Set enableBackgroundStream to " + enable);
        this.enableBackgroundStream = enable;
    }

    @Override
    public void setAdvancedSetting(AdvancedSetting setting) {
        Ln.d("Set " + setting);
        this.settings.put(setting.getClass(), setting);
    }

    @Override
    public AdvancedSetting getAdvancedSetting(Class<? extends AdvancedSetting> clz) {
        return this.settings.get(clz);
    }

    @Override
    public boolean isHardwareAccelerationEnabled() {
        return hardwareCodecEnable;
    }

    @Override
    public void setHardwareAccelerationEnabled(boolean enable) {
        hardwareCodecEnable = enable;
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
        prompter.check(getContext(), null, result -> {
            H264LicenseAction action = result.getData();
            if (action == null || action == H264LicenseAction.ACCEPT) {
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
                        } else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
                            Ln.e("Already disconnected");
                            Queue.main.run(() -> callback.onComplete(ResultImpl.error("Already disconnected")));
                            Queue.serial.yield();
                            return;
                        }
                    }
                    callContext = new CallContext.Incoming(call, option, callback);
                    Ln.d("CallContext: " + callContext);
                    Queue.main.run(this::tryAcquirePermission);
                    Queue.serial.yield();
                });
            } else if (action == H264LicenseAction.DECLINE) {
                Ln.d("Decline H264 license");
                ResultImpl.errorInMain(callback, WebexError.from(WebexError.ErrorCode.DECLINE_H264_LICENSE));
            } else if (action == H264LicenseAction.VIEW_LICENSE) {
                Ln.d("View H264 license");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(H264LicensePrompter.LICENSE_URL));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.getContext().startActivity(browserIntent);
                ResultImpl.errorInMain(callback, WebexError.from(WebexError.ErrorCode.VIEW_H264_LICENSE));
            }
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
                } else if (call.getStatus() == Call.CallStatus.DISCONNECTED) {
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
            String url = call.getModel().getSelf() == null ? null : call.getModel().getSelf().getUrl();
            if (url == null) {
                Ln.e("Missing self participant URL");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing self participant URL")));
                Queue.serial.yield();
                return;
            }
            Queue.main.run(() -> {
                if (call.isSendingSharing()) {
                    stopSharing(call, result -> Ln.d("Stop sharing when call is endded."));
                    if (call.getMedia() != null) {
                        call.getMedia().leaveSharing(true);
                    }
                }
                service.leave(url, device, result -> {
                    WebexError error = result.getError();
                    if (error != null) {
                        if (error.is(WebexError.ErrorCode.CONFLICT_ERROR) || error.is(WebexError.ErrorCode.NETWORK_ERROR)) {
                            call.end(new CallObserver.CallErrorEvent(call, error));
                        } else {
                            Queue.main.run(() -> callback.onComplete(ResultImpl.error(error)));
                            Queue.serial.yield();
                            return;
                        }
                    }
                    doLocusResponse(new LocusResponse.Leave(call, result.getData(), callback), Queue.serial);
                });
            });

        });
    }

    void layout(CallImpl call, MediaOption.VideoLayout layout, @Nullable CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            String url = call.getModel().getSelf() == null ? null : call.getModel().getSelf().getUrl();
            if (url == null) {
                Ln.e("Missing self participant URL");
                if (callback != null) {
                    Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing self participant URL")));
                }
                Queue.serial.yield();
            } else {
                service.layout(url, device, layout, result -> {
                    if (callback != null) {
                        if (result.getError() != null) {
                            Queue.main.run(() -> callback.onComplete(ResultImpl.error(result.getError())));
                        } else {
                            Queue.main.run(() -> callback.onComplete(ResultImpl.success(null)));
                        }
                    }
                    Queue.serial.yield();
                });
            }
        });
    }

    void update(CallImpl call, boolean audio, boolean video, String localSdp, CompletionHandler<Void> callback) {
        Queue.serial.run(() -> {
            String url = call.getModel().getSelf().getMediaBaseUrl();
            String sdp = call.getModel().getLocalSdp();
            if (sdp == null) {
                sdp = localSdp;
            }
            String mediaId = device == null ? null : call.getModel().getMediaId(device.getDeviceUrl());
            if (url == null || sdp == null || mediaId == null) {
                Ln.e("Missing media data");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Missing media data")));
                Queue.serial.yield();
                return;
            }
            MediaEngineReachabilityModel reachabilities = reachability.getFeedback();
            MediaInfoModel media = new MediaInfoModel(localSdp == null ? sdp : localSdp, !audio, !video, reachabilities == null ? null : reachabilities.reachability);
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
            } else {
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
        metrics.post(this.getDevice(), list);
    }

    void keepAlive(CallImpl call) {
        String url = call.getModel().getKeepAliveUrl(device.getDeviceUrl());
        if (url == null) {
            Ln.e("Missing keepAlive URL");
            return;
        }
        service.keepalive(url, result -> {
        });
    }

    void startSharing(CallImpl call, CompletionHandler<Void> callback) {
        callContext = new CallContext.Sharing(call, callback);
        Ln.d("CallContext: " + callContext);
        final Intent intent = new Intent(context, AcquirePermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.putExtra(AcquirePermissionActivity.PERMISSION_TYPE, AcquirePermissionActivity.PERMISSION_SCREEN_SHOT);
        context.startActivity(intent);
    }

    void stopSharing(CallImpl call, CompletionHandler<Void> callback) {
        Ln.d("StopSharing " + call.getUrl());
        Queue.serial.run(() -> {
            if (call.getMedia() == null || !call.getMedia().hasSharing()) {
                Ln.e("Media option unsupport content share");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Media option unsupport content share")));
                Queue.serial.yield();
                return;
            }
            if (!call.isSharingFromThisDevice()) {
                Ln.e("Local share screen not start");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Local share screen not start")));
                Queue.serial.yield();
                return;
            }
            FloorModel floor = new FloorModel(call.getModel().getSelf(), call.getModel().getSelf(), FloorModel.Disposition.RELEASED);
            MediaShareModel mediaShare = new MediaShareModel(MediaShareModel.SHARE_CONTENT_TYPE, call.getModel().getMediaShareUrl(), floor);
            String url = mediaShare.getUrl();
            if (url == null) {
                Ln.e("Unsupport media share");
                Queue.main.run(() -> callback.onComplete(ResultImpl.error("Miss media share url")));
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.putExtra(AcquirePermissionActivity.PERMISSION_TYPE, AcquirePermissionActivity.PERMISSION_CAMERA_MIC);
        context.startActivity(intent);
    }

    private void doLocusResponse(LocusResponse response, Queue queue) {
        Ln.d("doLocusResponse: " + response);
        if (response instanceof LocusResponse.Call) {
            LocusModel model = ((LocusResponse.Call) response).getLocus();
            Ln.d("Connecting call: " + model.getCallUrl());
            CallAnalyzerReporter.shared.reportJoinResponseSuccess(((LocusResponse.Call) response).getCorrelationId(), model.getKey());
            CallImpl call = new CallImpl(((LocusResponse.Call) response).getCorrelationId(), model, this, device, ((LocusResponse.Call) response).getSession(), Call.Direction.OUTGOING, !model.isOneOnOne());
            if (call.isStatusIllegal()) {
                Ln.d("The previous session did not end");
                Queue.main.run(() -> {
                    WebexError error = WebexError.from("The previous session did not end");
                    ((LocusResponse.Call) response).getCallback().onComplete(ResultImpl.error(error));
                    call.end(new CallObserver.CallErrorEvent(call, error));
                    queue.yield();
                });
                return;
            }
            addCall(call);
            if (this.canceled) {
                this.canceled = false;
                Queue.main.run(() -> ((LocusResponse.Call) response).getCallback().onComplete(ResultImpl.error("The call be canceled by user")));
                this.hangup(call, result -> Ln.d("Call was hung up due to validate dial"));
                queue.yield();
                return;
            }
            Queue.main.run(() -> {
                if (call.getModel().isSelfInLobby()) {
                    call.startKeepAlive();
                } else {
                    call.startMedia();
                }
                ((LocusResponse.Call) response).getCallback().onComplete(ResultImpl.success(call));
                queue.yield();
            });
        } else if (response instanceof LocusResponse.Answer) {
            LocusResponse.Answer res = (LocusResponse.Answer) response;
            CallAnalyzerReporter.shared.reportJoinResponseSuccess(res.getCall().getCorrelationId(), res.getCall().getModel().getKey());
            res.getCall().update(res.getResult());
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        } else if (response instanceof LocusResponse.Leave) {
            LocusResponse.Leave res = (LocusResponse.Leave) response;
            LocusModel model = res.getResult();
            if (model != null) {
                res.getCall().update(model);
            }
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        } else if (response instanceof LocusResponse.Reject) {
            LocusResponse.Reject res = (LocusResponse.Reject) response;
            res.getCall().end(new CallObserver.LocalDecline(res.getCall()));
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        } else if (response instanceof LocusResponse.Ack) {
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
        } else if (response instanceof LocusResponse.Update) {
            LocusResponse.Update res = (LocusResponse.Update) response;
            LocusModel model = res.getResult();
            if (model != null) {
                res.getCall().update(model);
            }
            if (res.getCallback() != null) {
                Queue.main.run(() -> res.getCallback().onComplete(ResultImpl.success(null)));
            }
            queue.yield();
        } else if (response instanceof LocusResponse.MediaShare) {
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
            Ln.d("No LocusModel");
            return;
        }
        String url = model.getCallUrl();
        if (url == null) {
            Ln.d("LocusModel is missing call url");
            return;
        }
        Ln.d("doLocusEvent: " + url);
        //CallAnalyzerReporter.shared.reportClientNotificationReceived(model.getKey(), true);
        Queue.serial.run(() -> {
            if (!model.isOneOnOne() && model.isValid() && model.getConversationUrl() != null) {
                String spaceId = WebexId.from(model.getConversationUrl(), device).getBase64Id();
                if (!activeSpaceIds.contains(spaceId) && model.getFullState() != null && model.getFullState().isActive()) {
                    fire(new InternalSpace.InternalSpaceCallStarted(spaceId, model));
                    activeSpaceIds.add(spaceId);
                }
                if (activeSpaceIds.contains(spaceId) && model.getFullState() != null && !model.getFullState().isActive()) {
                    fire(new InternalSpace.InternalSpaceCallEnded(spaceId, model));
                    activeSpaceIds.remove(spaceId);
                }
            }
            CallImpl call = calls.get(url);
            if (call == null) {
                if (device != null && model.isIncomingCall() && model.isValid()) {
                    CallImpl incoming = new CallImpl(UUID.randomUUID().toString(), model, this, device, null, Call.Direction.INCOMING, !model.isOneOnOne());
                    addCall(incoming);
                    Ln.d("Receive incoming call: " + incoming.getUrl());
                    Queue.main.run(() -> {
                        IncomingCallListener listener = getIncomingCallListener();
                        if (listener != null) {
                            listener.onIncomingCall(incoming);
                        }
                    });
                } else {
                    Ln.d("Receive incoming call with invalid model");
                }
                Queue.serial.yield();
                return;
            }
            call.update(model);
            Queue.serial.yield();
        });
    }

    private void doActivityEvent(ActivityModel activity) {
        if (activity == null) {
            Ln.d("No ActivityModel");
            return;
        }
        String url = activity.getUrl();
        if (url == null) {
            Ln.d("ActivityModel is missing call url");
            return;
        }
        Ln.d("doActivityEvent: " + url);
        if (activity.getClientTempId() != null && activity.getClientTempId().startsWith(this.getPhoneId())) {
            Ln.d("The activity is sent by self");
            return;
        }
        String clusterId = this.device.getClusterId(url);

        ActivityModel.Verb verb = activity.getVerb();
        ObjectModel target = activity.getTarget();
        ObjectModel object = activity.getObject();

        if (verb == ActivityModel.Verb.add
                && object != null && object.isPerson()
                && target != null && target.isConversation()) {
            fire(new InternalMembership.InternalMembershipCreated(new InternalMembership(activity, clusterId), activity));
        } else if (verb == ActivityModel.Verb.leave
                && (object == null || object.isPerson())
                && target != null && target.isConversation()) {
            fire(new InternalMembership.InternalMembershipDeleted(new InternalMembership(activity, clusterId), activity));
        } else if ((activity.getVerb() == ActivityModel.Verb.assignModerator || activity.getVerb() == ActivityModel.Verb.unassignModerator)
                && object != null && object.isPerson()
                && target != null && target.isConversation()) {
            fire(new InternalMembership.InternalMembershipUpdated(new InternalMembership(activity, clusterId), activity));
        } else if (activity.getVerb() == ActivityModel.Verb.acknowledge
                && object != null && object.isActivity() && object.getId() != null
                && target != null && target.isConversation()) {
            fire(new InternalMembership.InternalMembershipMessageSeen(new InternalMembership(activity, clusterId), activity, new WebexId(WebexId.Type.MESSAGE, clusterId, object.getId()).getBase64Id()));
        } else if (activity.getVerb() == ActivityModel.Verb.create
                && object != null && object.isConversation() && object.getId() != null) {
            String base64Id = new WebexId(WebexId.Type.ROOM, clusterId, object.getId()).getBase64Id();
            webex.spaces().get(base64Id, result -> fire(result.getData() == null ? null : new InternalSpace.InternalSpaceCreated(result.getData(), activity)));
        } else if (activity.getVerb() == ActivityModel.Verb.update
                && object != null && object.isConversation()
                && target != null && target.isConversation() && target.getId() != null) {
            String base64Id = new WebexId(WebexId.Type.ROOM, clusterId, object.getId()).getBase64Id();
            webex.spaces().get(base64Id, result -> fire(result.getData() == null ? null : new InternalSpace.InternalSpaceUpdated(result.getData(), activity)));
        } else if ((activity.getVerb() == ActivityModel.Verb.post || activity.getVerb() == ActivityModel.Verb.share)
                && activity.getConversationId() != null && activity.getConversationUrl() != null) {
            ((MessageClientImpl) webex.messages()).doMessageReveived(activity, clusterId, message -> {
                fire(new InternalMessage.InternalMessageReceived(message, activity));
                // TODO Remove the deprecated event in next big release
                fire(new InternalMessage.InternalMessageArrived(message, activity));
            });
        } else if (activity.getVerb() == ActivityModel.Verb.update
                && activity.getConversationId() != null && activity.getConversationUrl() != null
                && object != null && object.isContent() && object.getId() != null) {
            ((MessageClientImpl) webex.messages()).doMessageUpdated(activity, this::fire);
        } else if (activity.getVerb() == ActivityModel.Verb.delete
                && object != null && object.isActivity() && object.getId() != null) {
            fire(new InternalMessage.InternalMessageDeleted(((MessageClientImpl) webex.messages()).doMessageDeleted(object.getId(), clusterId), activity));
        } else {
            Ln.d("Not a valid activity: " + url);
        }
    }

    private void fire(WebexEvent event) {
        Queue.main.run(() -> {
            if (event instanceof MembershipObserver.MembershipEvent && membershipObserver != null) {
                membershipObserver.onEvent((MembershipObserver.MembershipEvent) event);
            } else if (event instanceof SpaceObserver.SpaceEvent && spaceObserver != null) {
                spaceObserver.onEvent((SpaceObserver.SpaceEvent) event);
            } else if (event instanceof MessageObserver.MessageEvent && messageObserver != null) {
                messageObserver.onEvent((MessageObserver.MessageEvent) event);
            }
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

    public CallService getService() {
        return service;
    }

    void listActiveCalls() {
        Ln.d("Fetch call infos");
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

    private MediaCapability createCapability() {
        MediaCapability capability = new MediaCapability();
        capability.setAudioMaxRxBandwidth(getAudioMaxRxBandwidth());
        capability.setVideoMaxRxBandwidth(getVideoMaxRxBandwidth());
        capability.setVideoMaxTxBandwidth(getVideoMaxTxBandwidth());
        capability.setSharingMaxRxBandwidth(getSharingMaxRxBandwidth());
        capability.setHardwareCodecEnable(isHardwareAccelerationEnabled());
        capability.setHardwareVideoSetting(getHardwareVideoSettings());
        capability.setAudioEnhancementModels(audioEnhancementModels);
        capability.setEnableAudioBNR(isAudioBNREnable());
        capability.setAudioBRNMode(getAudioBNRMode());
        if (device != null) {
            capability.setDeviceSettings(device.getDeviceSettings());
        }
        capability.setDefaultCamera(WMEngine.Camera.fromFaceMode(getDefaultFacingMode()));
        capability.setAdvanceSettings(this.settings);
        return capability;
    }
}
