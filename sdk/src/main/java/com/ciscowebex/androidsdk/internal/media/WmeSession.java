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

package com.ciscowebex.androidsdk.internal.media;

import android.content.Context;
import android.util.Size;
import android.view.View;
import com.cisco.wme.appshare.ScreenShareContext;
import com.cisco.wx2.diagnostic_events.MediaLine;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.metric.MetricsHelper;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.media.device.MediaDeviceMananger;
import com.ciscowebex.androidsdk.utils.Json;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.*;
import com.webex.wseclient.WseEngine;
import me.helloworld.utils.Checker;

import java.util.*;

public class WmeSession implements ScreenShareContext.OnShareStoppedListener, MediaConnection.MediaConnectionListener {

    public enum State {
        INITIAL, READY, CONNECTED, DESTROYED;
    }

    private final MediaDeviceMananger deviceManager;
    private final Context context;
    private final MediaCapability capability;
    private State state = State.INITIAL;

    private WmeObserver observer;
    private Closure<String> sdpCallback;
    private String localSdp;
    private WmeSdpParsedInfo[] remoteSdp;
    private MediaConnection connection;

    private WmeTrack localAudioTrack = new WmeTrack(WmeTrack.Type.LocalAudio);
    private WmeTrack localVideoTrack = new WmeTrack(WmeTrack.Type.LocalVideo);
    private WmeTrack localSharingTrack = new WmeTrack(WmeTrack.Type.LocalSharing);
    private WmeTrack remoteAudioTrack = new WmeTrack(WmeTrack.Type.RemoteAudio);
    private WmeTrack remoteVideoTrack = new WmeTrack(WmeTrack.Type.RemoteVideo);
    private WmeTrack remoteSharingTrack = new WmeTrack(WmeTrack.Type.RemoteSharing);
    private List<WmeTrack> auxVideoTracks = new ArrayList<>();

    private String sharingId = "";
    private int auxStreamCount = 0;
    private WMEngine.Camera camera;

    private boolean iceConnected;
    private boolean iceFailed;
    private boolean usedTcpFallback;
    private volatile boolean receivedFirstAudioPacket;
    private volatile boolean receivedFirstVideoPacket;
    private volatile boolean receivedFirstShareContentPacket;
    private volatile boolean sentFirstAudioPacket;
    private volatile boolean sentFirstVideoPacket;
    private volatile boolean sentFirstShareContentPacket;
    private Date firstAudioPacketReceivedTime;
    private Date firstVideoPacketReceivedTime;
    private Map<Integer, Integer> latestMediaStatusMap = new HashMap<>();

    WmeSession(Context context, MediaDeviceMananger devManager, MediaCapability capability) {
        this.deviceManager = devManager;
        this.context = context;
        this.capability = capability;
        this.camera = capability.getDefaultCamera();
    }

    void setup() {
        Ln.d("Setup wme media session");
        if (connection != null || state != State.INITIAL) {
            Ln.w("Media Connection has already been created");
            return;
        }
        connection = new MediaConnection();
        connection.setListener(this);
        if (capability.hasAudio()) {
            connection.addMedia(WMEngine.Media.Audio.type(), getDirection(WMEngine.Media.Audio), WMEngine.Media.Audio.mid(), "");
        }
        if (capability.hasVideo()) {
            connection.addMedia(WMEngine.Media.Video.type(), getDirection(WMEngine.Media.Video), WMEngine.Media.Video.mid(), "");
        }
        if (capability.hasSharing()) {
            connection.addMedia(WMEngine.Media.Sharing.type(), getDirection(WMEngine.Media.Sharing), WMEngine.Media.Sharing.mid(), "");
            ScreenShareContext.getInstance().registerCallback(this);
        }
        capability.setupConnection(connection);
        for (int i = 0; i < capability.getMaxNumberStreams(); i++) {
            WmeTrack track = new WmeTrack(WmeTrack.Type.AuxVideo);
            auxVideoTracks.add(track);
        }
        state = State.READY;
    }

    public void launch(WmeObserver observer) {
        Queue.main.run(() -> {
            Ln.d("Launch wme media session: " + state);
            if (state != State.READY) {
                return;
            }
            this.observer = observer;
            latestMediaStatusMap.clear();
            deviceManager.register();
            localAudioTrack.start();
            remoteAudioTrack.start();
            localVideoTrack.start();
            remoteVideoTrack.start();
            if (!localSharingTrack.isSending()) {
                remoteSharingTrack.start();
            }
            state = State.CONNECTED;
        });
    }

    public void destroy() {
        Ln.d("Destroy wme media session");
        state = State.DESTROYED;
        latestMediaStatusMap.clear();
        if (connection != null && this.isMutedByLocal(WmeTrack.Type.LocalAudio)) {
            // Workaround for SPARK-93618, to reset the mute status of WME internal audio engine.
            unmute(WmeTrack.Type.LocalAudio);
        }
        if (localAudioTrack.isReady()) {
            localAudioTrack.stop(true);
            localAudioTrack.release();
        }
        if (remoteAudioTrack.isReady()) {
            remoteAudioTrack.stop(true);
            remoteAudioTrack.release();
        }
        if (localVideoTrack.isReady()) {
            localVideoTrack.stop(true);
            localVideoTrack.release();
        }
        if (remoteVideoTrack.isReady()) {
            remoteVideoTrack.stop(true);
            remoteVideoTrack.release();
        }
        if (localSharingTrack.isReady()) {
            localSharingTrack.stop(true);
            localSharingTrack.release();
        }
        if (remoteSharingTrack.isReady()) {
            remoteSharingTrack.stop(true);
            remoteSharingTrack.release();
        }
        if (getCapability().hasSharing()) {
            ScreenShareContext.getInstance().unregisterCallback(this);
        }
        if (connection != null) {
            connection.stopMediaLogging();
            connection.setListener(null);
            connection.stop();
            connection.destroy();
            connection = null;
        }
        localSdp = null;
        remoteSdp = null;
        localAudioTrack.clear();
        localVideoTrack.clear();
        localSharingTrack.clear();
        remoteAudioTrack.clear();
        remoteVideoTrack.clear();
        remoteSharingTrack.clear();
        deviceManager.unregister();
    }

    public State getState() {
        return state;
    }

    public String getLocalSdpOffer() {
        return localSdp;
    }

    public void createLocalSdpOffer(Closure<String> callback) {
        Ln.d("createLocalSdpOffer on: " + connection);
        if (connection != null) {
            localSdp = null;
            sdpCallback = callback;
            long result = connection.createOffer();
            Ln.d("createLocalSdpOffer result = " + result);
        }
    }

    public void receiveRemoteAnswer(String sdp) {
        Ln.d("receiveRemoteAnswer: " + sdp);
        if (!Checker.isEmpty(sdp)) {
            if (connection != null) {
                WmeSdpParsedInfo[] sdpInfos = connection.setReceivedSDP(MediaConnection.SDPType.Answer, sdp);
                updateTracksAddressPort();
                for (WmeSdpParsedInfo sdpInfo : sdpInfos) {
                    if (sdpInfo.mediaType == com.webex.wme.MediaConnection.MediaType.Video) {
                        Ln.d("Remote SDP for video is multistream? " + sdpInfo.isMultistream);
                        //this.isCompositedVideoStream = !sdpInfo.isMultistream;
                    }
                    compareAndUpdateTracks(sdpInfo);
                }
                remoteSdp = sdpInfos;
                connection.startMediaLogging(1000);
            }
        }
    }

    private void compareAndUpdateTracks(WmeSdpParsedInfo sdp) {
        Ln.d("compareAndUpdateTracks with remote SDP");
        MediaConnection.MediaType type = sdp.mediaType;
        MediaConnection.MediaDirection direction = sdp.remoteNegotiatedDirection;
        if (!Checker.isEmpty(remoteSdp)) {
            for (WmeSdpParsedInfo info : remoteSdp) {
                if (info.mediaType == type && info.remoteNegotiatedDirection == direction) {
                    return;
                }
            }
        }
        Ln.d("compareAndUpdateTracks with remote SDP: %s, %s", type, direction);
        if (type == MediaConnection.MediaType.Audio) {
            localAudioTrack.setRemoteSDPDirection(direction);
            remoteAudioTrack.setRemoteSDPDirection(direction);
            if (localAudioTrack.isReady()) {
                if (localAudioTrack.isSdpAllowSending()) {
                    localAudioTrack.start();
                } else {
                    localAudioTrack.stop(false);
                }
            }
            if (remoteAudioTrack.isReady()) {
                if (remoteAudioTrack.isSdpAllowReceiving()) {
                    remoteAudioTrack.start();
                } else {
                    remoteAudioTrack.stop(false);
                }
            }
        } else if (type == MediaConnection.MediaType.Video) {
            localVideoTrack.setRemoteSDPDirection(direction);
            remoteVideoTrack.setRemoteSDPDirection(direction);
            if (localVideoTrack.isReady()) {
                if (localVideoTrack.isSdpAllowSending()) {
                    localVideoTrack.start();
                } else {
                    localVideoTrack.stop(false);
                }
            }
            if (remoteVideoTrack.isReady()) {
                if (remoteVideoTrack.isSdpAllowReceiving()) {
                    remoteVideoTrack.start();
                } else {
                    remoteVideoTrack.stop(false);
                }
            }
        } else if (type == MediaConnection.MediaType.Sharing) {
            localSharingTrack.setRemoteSDPDirection(direction);
            remoteSharingTrack.setRemoteSDPDirection(direction);
            if (capability.hasSharing() && remoteSharingTrack.isReady()) {
                if (remoteSharingTrack.isSdpAllowReceiving()) {
                    if (localSharingTrack.isSending()) {
                        Ln.d("Skipped - local screen share has already started");
                        return;
                    }
                    remoteSharingTrack.start();
                } else {
                    remoteSharingTrack.stop(false);
                }
            }
        }
    }

    public MediaCapability getCapability() {
        return capability;
    }

    public MediaDeviceMananger getMediaDeviceManager(){
        return deviceManager;
    }

    private boolean hasMedia(WmeTrack.Type type) {
        if (type == WmeTrack.Type.LocalAudio || type == WmeTrack.Type.RemoteAudio) {
            return capability.hasAudio();
        } else if (type == WmeTrack.Type.LocalVideo || type == WmeTrack.Type.RemoteVideo || type == WmeTrack.Type.Preview || type == WmeTrack.Type.AuxVideo) {
            return capability.hasVideo();
        } else if (type == WmeTrack.Type.LocalSharing || type == WmeTrack.Type.RemoteSharing) {
            return capability.hasSharing();
        }
        return false;
    }

    private MediaConnection.MediaDirection getDirection(WMEngine.Media type) {
        if (type == WMEngine.Media.Audio) {
            return MediaConnection.MediaDirection.SendRecv;
        } else if (type == WMEngine.Media.Video) {
            if (localVideoTrack.getRenderView() != null || remoteVideoTrack.getRenderView() != null) {
                return MediaConnection.MediaDirection.SendRecv;
            }
            return MediaConnection.MediaDirection.Inactive;
        } else if (type == WMEngine.Media.Sharing) {
            if (remoteSharingTrack.getRenderView() != null) {
                return MediaConnection.MediaDirection.SendRecv;
            } else if (capability.hasSharing()) {
                return MediaConnection.MediaDirection.SendOnly;
            }
            return MediaConnection.MediaDirection.Inactive;
        }
        return MediaConnection.MediaDirection.SendRecv;
    }

    public void addRenderView(View view, WmeTrack.Type type) {
        addRenderView(view, type, WMEngine.MAIN_VID);
    }

    public void addRenderView(View view, WmeTrack.Type type, long vid) {
        Ln.d("addRenderView %s, %s, %s", view, type, vid);
        if (view == null) {
            Ln.d("Skipped - render view is not ready");
            return;
        }
        WmeTrack track = getTrack(type, vid);
        if (track == null) {
            Ln.d("Video track is not ready");
            return;
        }
        track.addView(view);
    }

    public void removeRenderView(View view, WmeTrack.Type type) {
        removeRenderView(view, type, WMEngine.MAIN_VID);
    }

    public void removeRenderView(View view, WmeTrack.Type type, long vid) {
        Ln.d("removeRenderView %s, %s, %s", view, type, vid);
        if (view == null) {
            Ln.d("Skipped - render view or video track are not ready");
            return;
        }
        WmeTrack track = getTrack(type, vid);
        if (track == null) {
            Ln.d("Video track is not ready");
            return;
        }
        track.removeView(view);
    }

    public void removeAllRenderViews(WmeTrack.Type type) {
        Ln.d("removeAllRenderViews %s", type);
        WmeTrack track = getTrack(type, WMEngine.MAIN_VID);
        if (track == null) {
            Ln.d("Video track is not ready");
            return;
        }
        track.removeAllViews();
    }

    public View getRenderView(WmeTrack.Type type) {
        WmeTrack track = getTrack(type, WMEngine.MAIN_VID);
        return track == null ? null : track.getRenderView();
    }

    public Size getRenderViewSize(WmeTrack.Type type) {
        return getRenderViewSize(type, WMEngine.MAIN_VID);
    }

    public Size getRenderViewSize(WmeTrack.Type type, long vid) {
        WmeTrack track = getTrack(type, vid);
        return track == null ? new Size(0, 0) : new Size(track.getVideoWidth(), track.getVideoHeight());
    }

    public void setVideoRenderMode(WmeTrack.Type type, MediaTrack.ScalingMode mode) {
        WmeTrack track = getTrack(type, WMEngine.MAIN_VID);
        if (track != null && track.getTrack() != null) {
            Ln.d("Media.setVideoRenderMode: " + mode + ", for " + type);
            track.getTrack().SetRenderMode(mode);
        }
    }

    public WmeTrack getTrack(WmeTrack.Type type, long vid) {
        if (type == WmeTrack.Type.LocalAudio) {
            return localAudioTrack;
        } else if (type == WmeTrack.Type.LocalVideo || type == WmeTrack.Type.Preview) {
            return localVideoTrack;
        } else if (type == WmeTrack.Type.LocalSharing) {
            return localSharingTrack;
        } else if (type == WmeTrack.Type.RemoteAudio) {
            return remoteAudioTrack;
        } else if (type == WmeTrack.Type.RemoteVideo) {
            if (vid == WMEngine.MAIN_VID) {
                return remoteVideoTrack;
            }
            return getAuxVideoTrack(vid);
        } else if (type == WmeTrack.Type.RemoteSharing) {
            return remoteSharingTrack;
        } else if (type == WmeTrack.Type.AuxVideo) {
            return getAuxVideoTrack(vid);
        } else {
            return null;
        }
    }

    private void setMediaTrackMuted(WmeTrack track, boolean mute) {
        Ln.d("setMediaTrackMuted track: " + track + ", mute: " + mute);
        if (track == null || !track.isReady()) {
            return;
        }
        long result = mute ? track.getTrack().Mute() : track.getTrack().Unmute();
        if (result != 0) {
            MediaError error = MediaError.of(result);
            if (observer != null && error != null) {
                observer.onError(error);
            }
        } else {
            if (track.isLocalTrack()) {
                track.setSending(!mute);
            } else {
                track.setReceiving(!mute);
            }
            if (observer != null) {
                observer.onTrackMuted(track.getType(), track.getVid(), mute);
            }
        }

        if (observer != null && mute) {
            if (track.getType() == WmeTrack.Type.LocalAudio) {
                observer.onMediaTxStop(WMEngine.Media.Audio);
            }
            else if (track.getType() == WmeTrack.Type.LocalVideo) {
                observer.onMediaTxStop(WMEngine.Media.Video);
            }
        }
    }

    public void mute(WmeTrack.Type type) {
        mute(type, WMEngine.MAIN_VID);
    }

    public void mute(WmeTrack.Type type, long vid) {
        if (!hasMedia(type)) {
            Ln.d("Media is not presented");
            return;
        }
        WmeTrack track = getTrack(type, vid);
        setMediaTrackMuted(track, true);
    }

    public void unmute(WmeTrack.Type type) {
        unmute(type, WMEngine.MAIN_VID);
    }

    public void unmute(WmeTrack.Type type, long vid) {
        if (!hasMedia(type)) {
            Ln.d("Media is not presented");
            return;
        }
        WmeTrack track = getTrack(type, vid);
        setMediaTrackMuted(track, false);
    }

    public boolean isMutedByLocal(WmeTrack.Type type) {
        return isMutedByLocal(type, WMEngine.MAIN_VID);
    }

    public boolean isMutedByLocal(WmeTrack.Type type, long vid) {
        if (!hasMedia(type)) {
            Ln.d("Media is not presented");
            return false;
        }
        WmeTrack track = getTrack(type, vid);
        if (track == null) {
            return false;
        }
        return track.isLocalTrack() ? !track.isSending() : !track.isReceiving();
    }

    public boolean isMutedByRemote(WmeTrack.Type type) {
        return isMutedByRemote(type, WMEngine.MAIN_VID);
    }

    public boolean isMutedByRemote(WmeTrack.Type type, long vid) {
        if (!hasMedia(type)) {
            Ln.d("Media is not presented");
            return false;
        }
        WmeTrack track = getTrack(type, vid);
        if (track == null) {
            return false;
        }
        return !track.isLocalTrack() && !track.isSending();
    }

    public void setCamera(WMEngine.Camera camera) {
        Ln.d("setCamera: " + camera);
        if (!capability.hasSupport(MediaConstraint.SendVideo)) {
            Ln.d("Media is not presented");
            return;
        }
        if (localVideoTrack == null || !localVideoTrack.isReady()) {
            Ln.d("Video track is not ready");
            return;
        }
        if (camera == null || camera == this.camera) {
            return;
        }
        this.camera = camera;
        if (applyCamera() && observer != null) {
            observer.onCameraSwitched();
        }
    }

    public WMEngine.Camera getCamera() {
        return camera;
    }

    private boolean applyCamera() {
        Ln.d("applyCamera %s to %s", camera, localVideoTrack);
        if (localVideoTrack == null) {
            Ln.d("Video track is not ready");
            return false;
        }
        if (deviceManager != null) {
            DeviceManager.MediaDevice dev = deviceManager.getCamera(camera);
            if (dev != null) {
                MediaError error = MediaError.of(localVideoTrack.getTrack().SetCaptureDevice(dev));
                if (error != null && observer != null) {
                    observer.onError(error);
                    return false;
                }
            }
        }
        return true;
    }

    public void setAudioVolume(int volume) {
        Ln.d("setAudioVolume(), volume = %d", volume);
        if (remoteAudioTrack != null && remoteAudioTrack.getTrack() != null) {
            remoteAudioTrack.getTrack().setTrackVolume(volume);
        }
    }

    public int getAudioVolume() {
        int volume = 0;
        if (remoteAudioTrack != null && remoteAudioTrack.getTrack() != null) {
            volume = remoteAudioTrack.getTrack().getTrackVolume();
        }
        return volume;
    }

    public void headsetPluggedIn() {
        Ln.d("headsetPluggedIn");
        if (localAudioTrack != null && localAudioTrack.getTrack() != null) {
            localAudioTrack.getTrack().AudioDeviceNotification("WmeAudioAndroid_HeadsetPlugin", 0, 0);
        }
    }

    public void headsetPluggedOut() {
        Ln.d("headsetPluggedOut");
        if (localAudioTrack != null && localAudioTrack.getTrack() != null) {
            localAudioTrack.getTrack().AudioDeviceNotification("WmeAudioAndroid_HeadsetPlugout", 0, 0);
        }
    }

    public AudioPlaybackStreamType getAudioPlaybackStreamType() {
        AudioPlaybackStreamType result = null;
        if (connection != null) {
            MediaConfig.AudioConfig audioConfig = connection.GetAudioConfig(WMEngine.Media.Audio.mid());
            result = AudioPlaybackStreamType.from((int) audioConfig.GetPlaybackStreamMode());
            Ln.i("getAudioPlaybackStreamType() result: %s", result);
        } else {
            Ln.i("getAudioPlaybackStreamType() defaulting to result: %s (as mediaConnection is null)", result);
        }
        return result == null ? AudioPlaybackStreamType.VOICE_CALL : result;
    }

    public void updateVideoTracks(View local, View remote) {
        Ln.d("updateVideoTrack local=%s, remote=%s", local, remote);
        if (connection == null) {
            Ln.d("Media Connection has not been created");
            return;
        }
        if (capability.hasVideo()) {
            if (local != null && remote != null) {
                if (local != localVideoTrack.getRenderView()) {
                    localVideoTrack.stop(true);
                    localVideoTrack.removeAllViews();
                    localVideoTrack.addView(local);
                }
                if (remote != remoteVideoTrack.getRenderView()) {
                    remoteVideoTrack.stop(true);
                    remoteVideoTrack.removeAllViews();
                    remoteVideoTrack.addView(remote);
                }
                MediaConnection.MediaDirection direction = getDirection(WMEngine.Media.Video);
                localVideoTrack.setRemoteSDPDirection(direction);
                remoteVideoTrack.setRemoteSDPDirection(direction);
                connection.addMedia(WMEngine.Media.Video.type(), direction, WMEngine.Media.Video.mid(), "");
                localVideoTrack.start();
                remoteVideoTrack.start();
            } else {
                localVideoTrack.stop(true);
                localVideoTrack.removeAllViews();
                remoteVideoTrack.stop(true);
                remoteVideoTrack.removeAllViews();
                MediaConnection.MediaDirection direction = getDirection(WMEngine.Media.Video);
                localVideoTrack.setRemoteSDPDirection(direction);
                remoteVideoTrack.setRemoteSDPDirection(direction);
                connection.updateMedia(direction, WMEngine.Media.Video.mid());
            }
        }
    }

    public void updateSharingTracks(View view) {
        Ln.d("updateSharingTracks view=%s", view);
        if (connection == null) {
            Ln.d("Media Connection has not been created");
            return;
        }
        if (capability.hasSharing()) {
            if (view == null) {
                remoteSharingTrack.stop(true);
                remoteSharingTrack.removeAllViews();
                MediaConnection.MediaDirection direction = getDirection(WMEngine.Media.Video);
                remoteSharingTrack.setRemoteSDPDirection(direction);
                connection.updateMedia(direction, WMEngine.Media.Sharing.mid());
            } else {
                if (view != remoteSharingTrack.getRenderView()) {
                    remoteSharingTrack.stop(true);
                    remoteSharingTrack.removeAllViews();
                    remoteSharingTrack.addView(view);
                }
                MediaConnection.MediaDirection direction = getDirection(WMEngine.Media.Video);
                remoteSharingTrack.setRemoteSDPDirection(direction);
                connection.addMedia(WMEngine.Media.Sharing.type(), direction, WMEngine.Media.Sharing.mid(), "");
                remoteSharingTrack.start();
            }
        }
    }

    private void updateTracksAddressPort() {
        if (connection == null) {
            Ln.d("Media Connection has not been created");
            return;
        }
        MediaStatistics.AudioStatistics audioStatistics = connection.getAudioStatistics(WMEngine.Media.Audio.mid());
        if (audioStatistics != null) {
            if (localAudioTrack != null && localAudioTrack.getTrack() != null) {
                localAudioTrack.setIpAddress(audioStatistics.mConnection.localIp);
                localAudioTrack.setPort((int) audioStatistics.mConnection.uLocalPort);
            }
            if (remoteAudioTrack != null && remoteAudioTrack.getTrack() != null) {
                remoteAudioTrack.setIpAddress(audioStatistics.mConnection.remoteIp);
                remoteAudioTrack.setPort((int) audioStatistics.mConnection.uRemotePort);
            }
        }
        if (capability.hasVideo()) {
            MediaStatistics.VideoStatistics videoStatistics = connection.getVideoStatistics(WMEngine.Media.Video.mid());
            if (videoStatistics != null) {
                if (localVideoTrack != null && localVideoTrack.getTrack() != null) {
                    localVideoTrack.setIpAddress(videoStatistics.mConnection.localIp);
                    localVideoTrack.setPort((int) videoStatistics.mConnection.uLocalPort);
                }
                if (remoteVideoTrack != null && remoteVideoTrack.getTrack() != null) {
                    remoteVideoTrack.setIpAddress(videoStatistics.mConnection.remoteIp);
                    remoteVideoTrack.setPort((int) videoStatistics.mConnection.uRemotePort);
                }
            }
        }
        if (capability.hasSharing()) {
            MediaStatistics.SharingStatistics sharingStatistics = connection.getSharingStatistics(WMEngine.Media.Sharing.mid());
            if (sharingStatistics != null) {
                if (localSharingTrack != null && localSharingTrack.getTrack() != null) {
                    localSharingTrack.setIpAddress(sharingStatistics.mConnection.localIp);
                    localSharingTrack.setPort((int) sharingStatistics.mConnection.uLocalPort);
                }
                if (remoteSharingTrack != null && remoteSharingTrack.getTrack() != null) {
                    remoteSharingTrack.setIpAddress(sharingStatistics.mConnection.remoteIp);
                    remoteSharingTrack.setPort((int) sharingStatistics.mConnection.uRemotePort);
                }
            }
        }
    }

    public void joinSharing(String sharingId, boolean send) {
        Ln.d("joinSharing with " + sharingId + ", send: " + send);
        if (sharingId != null) {
            this.sharingId = sharingId;
            if (send && localSharingTrack != null && localSharingTrack.isReady()) {
                localSharingTrack.getTrack().SetScreenSharingID(this.sharingId);
                if (!localSharingTrack.isSending()) {
                    localSharingTrack.getTrack().Start(false);
                    localSharingTrack.setSending(true);
                }
                if (observer != null) {
                    observer.onMediaTxStart(WMEngine.Media.Sharing);
                }
            } else if (remoteSharingTrack != null && remoteSharingTrack.isReady() && !send) {
                remoteSharingTrack.getTrack().SetScreenSharingID(this.sharingId);
                remoteSharingTrack.start();
            }
        }
    }

    public void leaveSharing(boolean send) {
        Ln.d("leaveSharing with " + send);
        this.sharingId = "";
        if (send && localSharingTrack != null && localSharingTrack.isReady()) {
            localSharingTrack.getTrack().Stop();
            localSharingTrack.setSending(false);
            ScreenShareContext.getInstance().finit();
            localSharingTrack.getTrack().SetScreenSharingID(this.sharingId);
            if (observer != null) {
                observer.onMediaTxStop(WMEngine.Media.Sharing);
            }
        } else if (remoteSharingTrack != null && remoteSharingTrack.isReady() && !send) {
            remoteSharingTrack.getTrack().SetScreenSharingID(this.sharingId);
            remoteSharingTrack.stop(true);
        }
    }

    public int getAuxStreamCount() {
        return auxStreamCount;
    }

    public int subscribeAuxVideo(View view) {
        Ln.d("subscribeAuxVideo for " + view);
        int vid = -1;
        if (connection == null) {
            Ln.d("Media Connection has not been created");
            return vid;
        }
        for (WmeTrack track : auxVideoTracks) {
            if (track.getRenderView() == null || track.getVid() <= 0) {
                MediaSCR p = MediaSCR.p90;
                try {
                    vid = connection.subscribe(WMEngine.Media.Video.mid(), MediaConfig.WmeSubscribePolicy.ActiveSpeaker, p.maxFs, p.maxFps, p.maxBr, p.maxDpb, p.maxMbps, p.priority, p.grouping, p.duplicate, 0);
                } catch (IllegalArgumentException e) {
                    Ln.e(e, "error calling MediaConnection subscribe");
                }
                Ln.d("MediaConnection.subscribe: " + vid);
                track.addView(view);
                track.setVid(vid);
                return vid;
            }
        }
        return vid;
    }

    public void unsubscribeAuxVideo(int vid) {
        Ln.d("unsubscribeAuxVideo: " + vid);
        if (connection == null) {
            Ln.d("Media Connection has not been created");
            return;
        }
        WmeTrack track = getAuxVideoTrack(vid);
        if (track != null && track.getRenderView() != null && track.getVid() >= 1) {
            track.stop(true);
            track.release();
            track.clear();
            connection.unsubscribe(WMEngine.Media.Video.mid(), vid);
        }
    }

    private WmeTrack getAuxVideoTrack(long vid) {
        for (WmeTrack auxTrack : auxVideoTracks) {
            if (auxTrack.getVid() == vid) {
                return auxTrack;
            }
        }
        return null;
    }

    public void onSDPReady(MediaConnection.SDPType type, String sdp) {
        Ln.d("Media.onSDPReady, typ=" + type + ", sdp=" + sdp);
        localSdp = sdp;
        if (sdpCallback != null) {
            sdpCallback.invoke(sdp);
        }
    }

    @Override
    public void onMediaReady(int mid, MediaConnection.MediaDirection direction, MediaConnection.MediaType type, MediaTrack track) {
        Queue.main.run(() -> {
            Ln.d("Media.onMediaReady, track=" + track + ", mConn=" + connection + ", mid=" + mid + ", dir=" + direction + ", type=" + type);
            if (type == MediaConnection.MediaType.Video) {
                if (direction == MediaConnection.MediaDirection.SendOnly) {
                    localVideoTrack.init(track);
                    applyCamera();
                    if (capability.isCamera2Enabled()) {
                        WseEngine.EnableCamera2(context);
                    }
                    localVideoTrack.start();
                } else if (direction == MediaConnection.MediaDirection.RecvOnly) {
                    long vid = track.getVID();
                    Ln.d("onMediaReady track: " + vid);
                    if (vid == WMEngine.MAIN_VID) {
                        remoteVideoTrack.init(track);
                        if (getCapability().isHardwareCodecEnable()) {
                            MediaHelper.requestSCR(remoteVideoTrack.getTrack(), MediaSCR.p720);
                        } else {
                            MediaHelper.requestSCR(remoteVideoTrack.getTrack(), MediaSCR.p360);
                        }
                    } else {
                        WmeTrack auxTrack = getAuxVideoTrack(vid);
                        if (auxTrack != null) {
                            auxTrack.init(track);
                            auxTrack.start();
                        }
                    }
                }
            } else if (type == MediaConnection.MediaType.Audio) {
                WmeTrack audioTrack = direction == MediaConnection.MediaDirection.SendOnly ? localAudioTrack : remoteAudioTrack;
                audioTrack.init(track);
                audioTrack.start();
            } else if (type == MediaConnection.MediaType.Sharing) {
                if (direction == MediaConnection.MediaDirection.RecvOnly) {
                    remoteSharingTrack.init(track);
                    MediaHelper.requestSCR(remoteSharingTrack.getTrack(), MediaSCR.p1080);
                    if (!Checker.isEmpty(sharingId)) {
                        remoteSharingTrack.getTrack().SetScreenSharingID(sharingId);
                        remoteSharingTrack.start();
                    }
                } else if (direction == MediaConnection.MediaDirection.SendOnly) {
                    localSharingTrack.init(track);
                    if (!Checker.isEmpty(sharingId)) {
                        localSharingTrack.getTrack().SetScreenSharingID(sharingId);
                    }
                }
            }
        });
    }

    public void onSessionStatus(int mid, MediaConnection.MediaType mediaType, MediaConnection.ConnectionStatus status) {
        Ln.d("Media.onSessionStatus: mid=" + mid + ", mediaType=" + mediaType + ", ConnectionStatus=" + status);
        if (status == MediaConnection.ConnectionStatus.Connected) {
            updateTracksAddressPort();
        }

        if (status == MediaConnection.ConnectionStatus.Sent) {
            if (mediaType == MediaConnection.MediaType.Audio)
                setFirstAudioPacketSent(new Date());
            else if (mediaType == MediaConnection.MediaType.Video) {
                setFirstVideoPacketSent(new Date());
            }
        } else if (status == MediaConnection.ConnectionStatus.Received) {
            if (mediaType == MediaConnection.MediaType.Audio)
                setFirstAudioPacketReceived(new Date());
            else if (mediaType == MediaConnection.MediaType.Video) {
                setFirstVideoPacketReceived(new Date());
            } else if (mediaType == MediaConnection.MediaType.Sharing) {
                setFirstSharePacketReceived(new Date());
            }
        } else if (status == MediaConnection.ConnectionStatus.Connected
                || status == MediaConnection.ConnectionStatus.Reconnected) {
            iceConnected = true;
            iceConnectionStatusChange(mediaType, true);
            if (!usedTcpFallback && connection != null && state == State.CONNECTED) {
                MediaStatistics.AudioStatistics audio = connection.getAudioStatistics(WMEngine.Media.Audio.mid());
                MediaStatistics.VideoStatistics video = connection.getVideoStatistics(WMEngine.Media.Video.mid());
                String audioConnectionType = audio.mConnection.connectionType;
                String videoConnectionType = video.mConnection.connectionType;
                if ("TCP".equals(audioConnectionType) || "TCP".equals(videoConnectionType)) {
                    usedTcpFallback = true;
                }
            }
        } else if (status == MediaConnection.ConnectionStatus.Disconnected) {
            if (!iceConnected && !iceFailed) {
                iceFailed = true;
            }
            iceConnected = false;
            iceConnectionStatusChange(mediaType, false);
        } else if (status == MediaConnection.ConnectionStatus.Terminated) {
            iceConnected = false;
            iceConnectionStatusChange(mediaType, false);
        } else if (status == MediaConnection.ConnectionStatus.FileCaptureEnded) {

        }
    }

    @Override
    public void onMediaBlocked(int mid, int vid, boolean blocked) {
        Ln.d("Media.onMediaBlocked, mid=" + mid + ", vid = " + vid + ", blocked=" + blocked);
        WMEngine.Media type = WMEngine.Media.from(mid);
        if (!blocked) {
            if (type == WMEngine.Media.Video) {
                if (vid == WMEngine.MAIN_VID) {
                    if (!remoteVideoTrack.isSending()) {
                        remoteVideoTrack.setSending(true);
                        if (observer != null) {
                            observer.onRemoteVideoAvailable(true);
                        }
                    }
                } else {
                    WmeTrack auxTrack = getAuxVideoTrack(vid);
                    if (auxTrack != null && !auxTrack.isSending()) {
                        auxTrack.setSending(true);
                        if (observer != null) {
                            observer.onAuxVideoAvailable(vid, true);
                        }
                    }
                }
            } else if (type == WMEngine.Media.Sharing) {
                if (!remoteSharingTrack.isSending()) {
                    remoteSharingTrack.setSending(true);
                    if (observer != null) {
                        observer.onRemoteSharingAvailable(true);
                    }
                }
            }
        }

        if (observer != null) {
            if ((type == WMEngine.Media.Video && receivedFirstVideoPacket)
                    || (type == WMEngine.Media.Sharing && receivedFirstShareContentPacket)) {
                if (vid == WMEngine.MAIN_VID) {
                    if (type == WMEngine.Media.Video) {
                        observer.onMediaBlocked(WMEngine.Media.Video, blocked);
                    } else if (!blocked) {
                        observer.onMediaBlocked(WMEngine.Media.Sharing, false);
                    }
                }
            }
            if ((type == WMEngine.Media.Video || type == WMEngine.Media.Sharing) && vid == WMEngine.MAIN_VID) {
                if (blocked) {
                    if (latestMediaStatusMap.get(mid) != null) {
                        observer.onMediaRxStop(type, latestMediaStatusMap.get(mid));
                    }
                } else {
                    observer.onMediaRxStart(type, null);
                }
            }
        }
    }

    /**
     * References:
     * 1. https://wiki.cisco.com/display/WMEAPI/ActiveVideo
     * 2. https://wiki.cisco.com/display/WMEAPI/IWmeMediaConnectionSink#IWmeMediaConnectionSink-onmediastatus
     */
    @Override
    public synchronized void onMediaStatus(int mid, int vid, MediaConnection.MediaStatus status, boolean hasCSI, long csi) {
        WMEngine.Media type = WMEngine.Media.from(mid);
        Ln.d("Media.onMediaStatus, mid = %s (%d) vid = %d (%s) status = %s hasCsi = %s csi = %d", type, mid, vid, MediaHelper.getVideoStreamString(vid), status, hasCSI, csi);
        if ((type == WMEngine.Media.Video || type == WMEngine.Media.Sharing) && vid == WMEngine.MAIN_VID) {
            latestMediaStatusMap.put(mid, status.value());
        }

        if (status != MediaConnection.MediaStatus.Available) {
            if (type == WMEngine.Media.Video) {
                if (vid == WMEngine.MAIN_VID) {
                    if (remoteVideoTrack.isSending()) {
                        remoteVideoTrack.setSending(false);
                        if (observer != null) {
                            observer.onRemoteVideoAvailable(false);
                        }
                    }
                } else {
                    WmeTrack track = getAuxVideoTrack(vid);
                    if (track != null && track.isSending()) {
                        track.setSending(false);
                        if (observer != null) {
                            observer.onAuxVideoAvailable(vid, false);
                        }
                    }
                }
            } else if (type == WMEngine.Media.Sharing) {
                if (remoteSharingTrack.isSending()) {
                    remoteSharingTrack.setSending(false);
                    if (observer != null) {
                        observer.onRemoteSharingAvailable(false);
                    }
                }
            }
        }
        if (type == WMEngine.Media.Video && hasCSI) {
            this.OnCSIsChanged(mid, vid, new long[0], new long[]{csi});
        }

    }

    public void onAvailableMediaChanged(int mid, int count) {
        Ln.d("Media.onAvailableMediaChanged, mid = " + mid + ", count = " + count);
        WMEngine.Media type = WMEngine.Media.from(mid);
        if (type == WMEngine.Media.Video) {
            auxStreamCount = count;
            if (observer != null) {
                observer.onAvailableMediaChanged(count);
            }
        }
    }

    public void OnCSIsChanged(long mid, long vid, long[] oldCSIs, long[] newCSIs) {
        Ln.d("Media.OnCSIsChanged, mid = " + mid + ",  vid = " + vid + ", oldCSIs = " + Arrays.toString(oldCSIs) + ", newCSIs = " + Arrays.toString(newCSIs));
        WMEngine.Media type = WMEngine.Media.from(mid);
        if (type == WMEngine.Media.Video && observer != null) {
            if (vid == WMEngine.MAIN_VID) {
                observer.onActiveSpeakerChanged(oldCSIs, newCSIs);
            } else {
                observer.onCSIChanged(vid, oldCSIs, newCSIs);
            }
        }
    }

    public void onDecodeSizeChanged(int mid, int vid, int width, int height) {
        Ln.d("Media.onDecodeSizeChanged, mid = " + mid + ", vid = " + vid + ", width = " + width + ", height = " + height);
        if (mid == WMEngine.Media.Video.mid()) {
            WmeTrack track = vid == WMEngine.MAIN_VID ? remoteVideoTrack : getAuxVideoTrack(vid);
            if (track != null) {
                track.setVideoWidth(width);
                track.setVideoHeight(height);
                //track.updateRenderMode();
            }
        } else if (mid == WMEngine.Media.Sharing.mid()) {
            remoteSharingTrack.setVideoWidth(width);
            remoteSharingTrack.setVideoHeight(height);
        }
        if (observer != null) {
            observer.onMediaDecodeSizeChanged(WMEngine.Media.from(mid), vid, new Size(width, height));
        }
    }

    public void onRenderSizeChanged(int mid, int vid, MediaConnection.MediaDirection direction, MediaConnection.WmeVideoSizeInfo size) {
        Ln.d("Media.onRenderSizeChanged, mid = " + mid + ", vid = " + vid + ", direction = " + direction + ", size = " + Json.get().toJson(size));
        WmeTrack track = null;
        WMEngine.Media media = WMEngine.Media.from(mid);
        if (media == WMEngine.Media.Video) {
            if (direction == MediaConnection.MediaDirection.SendOnly) {
                track = localVideoTrack;
            }
        } else if (media == WMEngine.Media.Sharing) {
            track = localSharingTrack;
        }
        if (track != null) {
            int width = size.GetRealWidth();
            int height = size.GetRealHeight();
            track.setVideoWidth(width);
            track.setVideoHeight(height);
            if (observer != null) {
                observer.onMediaRenderSizeChanged(media, vid, new Size(width, height));
            }
        }
    }

    public void onShareStopped() {
        Ln.d("MediaSessionImpl.onShareStopped");
    }

    public void OnRequestAvatarForMute(boolean mute) {
        Ln.d("MediaSessionImpl.OnRequestAvatarForMute(), mute = " + mute);
    }

    public int onPerformanceStatus(MediaConnection.PerformanceStatus status, MediaConnection.PerformanceReason reason) {
        Ln.i("MediaSessionImpl.onPerformanceStatus: status=" + status.value() + ", reason=" + reason.value());
        return status.value();
    }

    public void OnEventReportReady(MediaConnection.EventMetricsType eventMetricsType, String eventMetric) {
        Ln.d("OnEventReportReady, type: '%s', metric: '%s'", eventMetricsType.toString(), eventMetric);
        if (eventMetricsType == MediaConnection.EventMetricsType.IceConnection_CheckList) {
            if (observer != null) {
                List<MediaLine> mediaLines = new ArrayList<>();
                boolean connectSuccess = MetricsHelper.convertWMEIceListToMediaLines(eventMetric, mediaLines);
                observer.onICEReportReady(connectSuccess, mediaLines);
            }
        } else if (eventMetricsType == MediaConnection.EventMetricsType.MediaQualityInterval_Event) {
            if (observer != null) {
                observer.onMediaQualityMetricsReady(eventMetric);
            }
        }
    }

    public void onNetworkStatus(MediaConnection.NetworkStatus networkStatus, MediaConnection.NetworkDirection networkDirection) {
        Ln.d("MediaSessionImpl.onNetworkStatus, status = " + networkStatus.toString());
    }

    public void onError(int errorCode) {
        Ln.d("MediaSession.onError, code = " + errorCode);
    }

    public void onMediaError(int mid, int vid, int errorCode) {
        Ln.i("MediaSessionImpl.onMediaError, vid = " + vid + ", mid = " + mid + ", errorCode = " + errorCode);
        if (observer != null) {
            observer.onMediaError(mid, vid, errorCode);
        }
    }

    public void onEncodeSizeChanged(int mid, int width, int height) {
        Ln.d("MediaSessionImpl.onEncodeSizeChanged, mid = " + mid + ", width = " + width + ", height = ", height);
    }

    public void setFirstAudioPacketSent(Date date) {
        Ln.i("MediaSessionImpl.setFirstAudioPacketSent, date = " + date.getTime() + ", this = " + this);
        sentFirstAudioPacket = true;
        if (observer != null) {
            observer.onMediaTxStart(WMEngine.Media.Audio);
        }
    }

    public void setFirstAudioPacketReceived(Date date) {
        Ln.i("MediaSessionImpl.setFirstAudioPacketReceived date = " + date.getTime());
        firstAudioPacketReceivedTime = date;
        receivedFirstAudioPacket = true;
        if (observer != null) {
            observer.onMediaRxStart(WMEngine.Media.Audio, null);
        }
    }

    public void setFirstVideoPacketSent(Date date) {
        Ln.i("MediaSessionImpl.setFirstVideoPacketSent date = " + date.getTime());
        sentFirstVideoPacket = true;
        if (observer != null) {
            observer.onMediaTxStart(WMEngine.Media.Video);
        }
    }

    public void setFirstVideoPacketReceived(Date date) {
        Ln.i("MediaSessionImpl.setFirstVideoPacketReceived date = " + date.getTime());
        firstVideoPacketReceivedTime = date;
        receivedFirstVideoPacket = true;
        if (observer != null) {
            observer.onMediaRxStart(WMEngine.Media.Video, null);
        }
    }

    public void setFirstSharePacketReceived(Date date) {
        Ln.i("MediaSessionImpl.setFirstSharePacketReceived date = %d, receivedFirstShareContentPacket = %b.", date.getTime(), receivedFirstShareContentPacket);
        if (!receivedFirstShareContentPacket) {
            if (observer != null) {
                observer.onMediaRxStart(WMEngine.Media.Sharing, null);
            }
            receivedFirstShareContentPacket = true;
        }
    }

    public void setFirstSharePacketSent(Date date) {
        Ln.i("MediaSessionImpl.setFirstSharePacketSent date = " + date.getTime());
    }

    private void iceConnectionStatusChange(MediaConnection.MediaType mediaType, boolean iceConnectionSucceeded) {
        if (mediaType == MediaConnection.MediaType.Audio) {
            // media session will update media status for each of media type, audio status for connect/disconnect is more reliable than the other two.
            if (observer != null) {
                if (iceConnectionSucceeded) {
                    observer.onICEComplete();
                } else {
                    observer.onICEFailed();
                }
            }
        } else {
            Ln.d("ConnectionStatus.Disconnected|Terminated for sharing|video, ignore it. Will wait for Audio");
        }
    }


}
