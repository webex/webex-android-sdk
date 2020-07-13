package com.ciscowebex.androidsdk.internal.media;

import android.content.Context;
import android.view.View;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.model.MediaEngineReachabilityModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.internal.media.device.MediaDeviceMananger;
import com.ciscowebex.androidsdk.phone.MediaOption;
import com.ciscowebex.androidsdk.phone.Phone;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.*;
import com.webex.wseclient.WseEngine;
import me.helloworld.utils.Checker;

import java.util.List;
import java.util.Map;

public class WMEngine implements StunTraceSink {

    public enum Camera {
        FRONT, BACK;

        public DeviceManager.CameraType toDeviceCamera() {
            if (this == FRONT) {
                return DeviceManager.CameraType.Front;
            }
            return DeviceManager.CameraType.Back;
        }

        public Phone.FacingMode toFaceMode() {
            if (this == WMEngine.Camera.BACK) {
                return Phone.FacingMode.ENVIROMENT;
            }
            return Phone.FacingMode.USER;
        }

        public static Camera fromFaceMode(Phone.FacingMode mode) {
            if (mode == Phone.FacingMode.ENVIROMENT) {
                return WMEngine.Camera.BACK;
            }
            return WMEngine.Camera.FRONT;
        }
    }

    public enum Media {
        Audio, Video, Sharing, Unknown;

        public int mid() {
            if (this == Audio) { return 1; }
            else if (this == Video) { return 2; }
            else if (this == Sharing) { return 3; }
            return -1;
        }

        public MediaConnection.MediaType type() {
            if (this == Audio) { return MediaConnection.MediaType.Audio; }
            else if (this == Video) { return MediaConnection.MediaType.Video; }
            else if (this == Sharing) { return MediaConnection.MediaType.Sharing; }
            return MediaConnection.MediaType.Unknown;
        }

        public static Media from(long mid) {
            if (mid == 1) { return Audio; }
            else if (mid == 2) { return Video; }
            else if (mid == 3) { return Sharing; }
            return Unknown;
        }
    }

    public static final long MAIN_VID = 0L;

    private Context context;
    private MediaDeviceMananger devManager;
    private MediaEngineReachabilityModel reachabilityResult;
    private Closure<MediaEngineReachabilityModel> reachabilityHandler;
    private WmeSession session;

    public WMEngine(Context context, MediaSessionAPI.TraceLevelMask level) {
        this.context = context;
        this.devManager = new MediaDeviceMananger(context, this);
        NativeMediaSession.initWME();
        MediaSessionAPI.init(context);
        StunTrace.INSTANCE.setStunTraceSink(this);
        MediaSessionAPI.INSTANCE.setTraceMask(level);

        TraceServer.INSTANCE.setTraceServerSink(new TraceServerSink() {
            @Override
            public void OnTraceServerResult(WmeStunTraceResult wmeStunTraceResult, String s) {
                Ln.d("OnTraceServerResult :Received reachability result: " + s);
                Queue.main.run(() -> {
                    if (Checker.isEmpty(s)) {
                        WMEngine.this.reachabilityResult = null;
                        if (WMEngine.this.reachabilityHandler != null) {
                            WMEngine.this.reachabilityHandler.invoke(null);
                            WMEngine.this.reachabilityHandler = null;
                        }
                    }
                    else {
                        WMEngine.this.reachabilityResult = Json.fromJson("{\"reachability\":" + s + "}", MediaEngineReachabilityModel.class);
                        if (WMEngine.this.reachabilityHandler != null) {
                            WMEngine.this.reachabilityHandler.invoke(WMEngine.this.reachabilityResult);
                            WMEngine.this.reachabilityHandler = null;
                        }
                    }
                });
            }

            @Override
            public void OnTraceServerEarlyResult(WmeStunTraceResult wmeStunTraceResult, String s) {
                Ln.d("OnTraceServerEarlyResult :Received reachability early result: " + s);
                if (!Checker.isEmpty(s)) {
                    Queue.main.run(() -> {
                        WMEngine.this.reachabilityResult = Json.fromJson("{\"reachability\":" + s + "}", MediaEngineReachabilityModel.class);
                        if (WMEngine.this.reachabilityHandler != null) {
                            WMEngine.this.reachabilityHandler.invoke(WMEngine.this.reachabilityResult);
                        }
                    });
                }
            }
        });
    }


    public void release() {
        Ln.i("MediaEngine released");
        if (session != null && session.getState() != WmeSession.State.DESTROYED) {
            session.destroy();
        }
        session = null;
        devManager = null;
        StunTrace.INSTANCE.setStunTraceSink(null);
        TraceServer.INSTANCE.setTraceServerSink(null);
        NativeMediaSession.unInitWME();
    }

    public String getVersion() {
        return MediaSessionAPI.INSTANCE.version() + " (MediaSession)";
    }

    public void setLoggingLevel(MediaSessionAPI.TraceLevelMask level) {
        Ln.d("MediaSessionEngine.setLoggingLevel(%d)", level.value());
        MediaSessionAPI.INSTANCE.setTraceMask(level);
    }

    public WmeSession createSession(MediaCapability capability, MediaOption option) {
        session = new WmeSession(context, devManager, capability);
        session.getCapability().addConstraints(MediaConstraint.SendAudio, MediaConstraint.ReceiveAudio);
        if (option.hasVideo()) {
            session.getCapability().addConstraints(MediaConstraint.SendVideo, MediaConstraint.ReceiveVideo);
            session.addRenderView(option.getLocalView(), WmeTrack.Type.LocalVideo);
            session.addRenderView(option.getRemoteView(), WmeTrack.Type.RemoteVideo);
        }
        if (option.hasSharing()) {
            session.getCapability().addConstraints(MediaConstraint.SendSharing, MediaConstraint.ReceiveSharing);
            session.addRenderView(option.getSharingView(), WmeTrack.Type.RemoteSharing);
        }
        session.setup();
        return session;
    }

    public WmeSession createPreviveSession(MediaCapability capability, View view) {
        capability.setDefaultCamera(Camera.FRONT);
        WmeSession session = new WmeSession(context, devManager, capability);
        session.getCapability().addConstraints(MediaConstraint.SendAudio, MediaConstraint.ReceiveAudio);
        session.getCapability().addConstraints(MediaConstraint.SendVideo, MediaConstraint.ReceiveVideo);
        session.addRenderView(view, WmeTrack.Type.Preview);
        session.setup();
        return session;
    }

    public WmeSession getSession() {
        return session;
    }

    public void performStunReachabilityCheck(Map<String, Map<String, List<String>>> clusterInfo, Closure<MediaEngineReachabilityModel> callback) {
        Ln.d("In performStunReachabilityCheck,cluster info is " + clusterInfo);
        reachabilityResult = null;
        if (reachabilityHandler != null) {
            Ln.d("Already started reachability check");
            callback.invoke(null);
            return;
        }
        if (Checker.isEmpty(clusterInfo)) {
            Ln.d("Input empty");
            callback.invoke(null);
            return;
        }
        String json = Json.get().toJson(clusterInfo);
        reachabilityHandler = callback;
        TraceServer.INSTANCE.startTraceServer(json, json.length());
    }

    public void clearReachabilityData() {
        reachabilityResult = null;
        TraceServer.INSTANCE.stopTraceServer();
    }

    public void setDisplayRotation(int rotation) {
        WseEngine.setDisplayRotation(rotation);
    }

    @Override
    public void OnResult(WmeStunTraceResult wmeStunTraceResult, String detail, long callId) {
        Ln.d("Stun Trace result, detail = " + detail + ", callId = " + callId);
    }

}
