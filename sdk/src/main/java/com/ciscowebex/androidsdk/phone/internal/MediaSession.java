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

import android.support.annotation.Nullable;
import android.util.Pair;
import android.util.Size;
import android.view.View;

import com.ciscowebex.androidsdk.internal.media.*;
import com.ciscowebex.androidsdk.internal.media.WmeTrack;
import com.ciscowebex.androidsdk.internal.media.device.MediaDeviceMananger;
import com.ciscowebex.androidsdk.internal.model.LocusParticipantDeviceModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.phone.Call;
import com.ciscowebex.androidsdk.phone.Phone;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.MediaConnection;
import com.webex.wme.MediaTrack;

public class MediaSession {

    interface MediaType {
    }

    static class MediaTypeVideo implements MediaType {

        private View localView;
        private View remoteView;

        public MediaTypeVideo(@Nullable Pair<View, View> views) {
            if (views != null) {
                localView = views.first;
                remoteView = views.second;
            }
        }
    }

    static class MediaTypeSharing implements MediaType {

        private View view;

        public MediaTypeSharing(@Nullable View view) {
            this.view = view;
        }

    }

    private interface SdpCallback {
        void onSdp(String sdp);
    }

    private final WmeSession session;

    private boolean localOnly;

    private boolean prepared;

    MediaSession(WmeSession session, boolean localOnly) {
        this.session = session;
        this.localOnly = localOnly;
    }

    public MediaCapability getCapability() {
        return session.getCapability();
    }

    MediaDeviceMananger getMediaDeviceManager() {
        return session.getMediaDeviceManager();
    }

    public boolean isRunning() {
        return session.getState() == WmeSession.State.CONNECTED;
    }

    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public void startPreview() {
        if (localOnly) {
            WmeTrack track = session.getTrack(WmeTrack.Type.Preview, WMEngine.MAIN_VID);
            track.setRemoteSDPDirection(MediaConnection.MediaDirection.SendRecv);
            track.start();
        }
    }

    public void stopPreview() {
        if (localOnly) {
            WmeTrack track = session.getTrack(WmeTrack.Type.Preview, WMEngine.MAIN_VID);
            track.stop(true);
            session.destroy();
        }
    }

    public void startCloud(CallImpl call) {
        if (!localOnly) {
            session.launch(new MediaObserver(call));
            Queue.main.run(() -> {
                WmeTrack track = session.getTrack(WmeTrack.Type.RemoteVideo, WMEngine.MAIN_VID);
                if (track != null && track.getTrack() != null && call != null && call.getModel() != null) {
                    LocusParticipantDeviceModel device = call.getModel().getMyDevice();
                    if (device == null) {
                        Ln.d("Cannot find self device for call: " + call);
                        return;
                    }
                    if (device.isServerComposed()) {
                        Ln.d("Set the remote video render mode to CropFill for composed video");
                        track.getTrack().SetRenderMode(MediaTrack.ScalingMode.CropFill);
                    }
                }
            });
        }
    }

    public void stopCloud() {
        if (!localOnly) {
            session.destroy();
        }
    }

    public void joinSharing(String id, boolean send) {
        if (hasSharing()) {
            session.joinSharing(id, send);
        }
    }

    public void leaveSharing(boolean send) {
        if (hasSharing()) {
            session.leaveSharing(send);
        }
    }

    public int subscribeAuxVideo(View view) {
        if (isRunning()) {
            return session.subscribeAuxVideo(view);
        }
        return -1;
    }

    public void unsubscribeAuxVideo(int vid) {
        if (vid >= 0) {
            session.unsubscribeAuxVideo(vid);
        }
    }

    public void addAuxVideoView(View view, int vid) {
        session.addRenderView(view, WmeTrack.Type.AuxVideo, vid);
    }

    public void removeAuxVideoView(View view, int vid) {
        session.removeRenderView(view, WmeTrack.Type.AuxVideo, vid);
    }

    public Size getAuxVideoViewSize(int vid) {
        return session.getRenderViewSize(WmeTrack.Type.AuxVideo, vid);
    }

    public boolean isAuxVideoLocalMuted(int vid) {
        return session.isMutedByLocal(WmeTrack.Type.AuxVideo, vid);
    }

    public boolean isAuxVideoRemoteMuted(int vid) {
        return session.isMutedByRemote(WmeTrack.Type.AuxVideo, vid);
    }

    public void muteAuxVideo(int vid) {
        session.mute(WmeTrack.Type.AuxVideo, vid);
    }

    public void unmuteAuxVideo(int vid) {
        session.unmute(WmeTrack.Type.AuxVideo, vid);
    }

    public int getAuxStreamCount() {
        return session.getAuxStreamCount();
    }

    public void update(MediaType type) {
        if (type instanceof MediaTypeVideo) {
            session.updateVideoTracks(((MediaTypeVideo) type).localView, ((MediaTypeVideo) type).remoteView);
        } else if (type instanceof MediaTypeSharing) {
            session.updateSharingTracks(((MediaTypeSharing) type).view);
        }
    }

    public synchronized String getLocalSdp() {
        session.createLocalSdpOffer(sdp -> {
            synchronized (this) {
                this.notifyAll();
            }
        });
        while (session.getLocalSdpOffer() == null) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Ln.e(e);
            }
        }
        return session.getLocalSdpOffer();
    }

    public void setRemoteSdp(String sdp) {
        session.receiveRemoteAnswer(sdp);
    }

    public boolean hasAudio() {
        return session.getCapability().hasAudio();
    }

    public boolean hasVideo() {
        return session.getCapability().hasVideo();
    }

    public boolean hasSharing() {
        return session.getCapability().hasSharing();
    }

    public Size getLocalVideoViewSize() {
        return session.getRenderViewSize(WmeTrack.Type.LocalVideo);
    }

    public Size getRemoteVideoViewSize() {
        return session.getRenderViewSize(WmeTrack.Type.RemoteVideo);
    }

    public Size getLocalSharingViewSize() {
        return session.getRenderViewSize(WmeTrack.Type.LocalSharing);
    }

    public Size getRemoteSharingViewSize() {
        return session.getRenderViewSize(WmeTrack.Type.RemoteSharing);
    }

    public void setRemoteVideoRenderMode(Call.VideoRenderMode mode) {
        MediaTrack.ScalingMode scalingMode;
        switch (mode) {
            case Fit:
                scalingMode = MediaTrack.ScalingMode.LetterBox;
                break;
            case StretchFill:
                scalingMode = MediaTrack.ScalingMode.Fill;
                break;
            default:
                scalingMode = MediaTrack.ScalingMode.CropFill;
        }
        session.setVideoRenderMode(WmeTrack.Type.RemoteVideo, scalingMode);
    }

    public Pair<View, View> getVideoViews() {
        View local = session.getRenderView(WmeTrack.Type.LocalVideo);
        View remote = session.getRenderView(WmeTrack.Type.RemoteVideo);
        return (local == null || remote == null) ? null : new Pair<>(local, remote);
    }

    public View getSharingView() {
        return session.getRenderView(WmeTrack.Type.RemoteSharing);
    }

    public void setLocalAudioSending(boolean sending) {
        if (sending) {
            session.unmute(WmeTrack.Type.LocalAudio);
        } else {
            session.mute(WmeTrack.Type.LocalAudio);
        }
    }

    public void setRemoteAudioReceiving(boolean receiving) {
        if (receiving) {
            session.unmute(WmeTrack.Type.RemoteAudio);
        } else {
            session.mute(WmeTrack.Type.RemoteAudio);
        }
    }

    public boolean isLocalAudioSending() {
        return !session.isMutedByLocal(WmeTrack.Type.LocalAudio);
    }

    public boolean isRemoteAudioReceiving() {
        return !session.isMutedByLocal(WmeTrack.Type.RemoteAudio);
    }

    public boolean isRemoteAudioSending() {
        return !session.isMutedByRemote(WmeTrack.Type.RemoteAudio);
    }

    public void setLocalVideoSending(boolean sending) {
        if (sending) {
            session.unmute(WmeTrack.Type.LocalVideo);
        } else {
            session.mute(WmeTrack.Type.LocalVideo);
        }
    }

    public void setRemoteVideoReceiving(boolean receiving) {
        if (receiving) {
            session.unmute(WmeTrack.Type.RemoteVideo);
        } else {
            session.mute(WmeTrack.Type.RemoteVideo);
        }
    }

    public boolean isLocalVideoSending() {
        return !session.isMutedByLocal(WmeTrack.Type.LocalVideo);
    }

    public boolean isRemoteVideoReceiving() {
        return !session.isMutedByLocal(WmeTrack.Type.RemoteVideo);
    }

    public boolean isRemoteVideoSending() {
        return !session.isMutedByRemote(WmeTrack.Type.RemoteVideo);
    }

    public void setLocalSharingSending(boolean sending) {
        if (sending) {
            session.unmute(WmeTrack.Type.LocalSharing);
        } else {
            session.mute(WmeTrack.Type.LocalSharing);
        }
    }

    public void setRemoteSharingReceiving(boolean receiving) {
        if (receiving) {
            session.unmute(WmeTrack.Type.RemoteSharing);
        } else {
            session.mute(WmeTrack.Type.RemoteSharing);
        }
    }

    public boolean isLocalSharingSending() {
        return !session.isMutedByLocal(WmeTrack.Type.LocalSharing);
    }

    public boolean isRemoteSharingReceiving() {
        return !session.isMutedByLocal(WmeTrack.Type.RemoteSharing);
    }

    public boolean isRemoteSharingSending() {
        return !session.isMutedByRemote(WmeTrack.Type.RemoteSharing);
    }

    public void setFacingMode(Phone.FacingMode facingMode) {
        session.setCamera(WMEngine.Camera.fromFaceMode(facingMode));
    }

    public Phone.FacingMode getFacingMode() {
        return session.getCamera().toFaceMode();
    }

    public void prepareToEnterVideoInterruption() {
        WmeTrack localVideo = session.getTrack(WmeTrack.Type.LocalVideo, WMEngine.MAIN_VID);
        if (localVideo != null) {
            localVideo.stop(true);
        }
        WmeTrack remoteVideo = session.getTrack(WmeTrack.Type.RemoteVideo, WMEngine.MAIN_VID);
        if (remoteVideo != null) {
            remoteVideo.stop(true);
        }
        WmeTrack remoteShare = session.getTrack(WmeTrack.Type.RemoteSharing, WMEngine.MAIN_VID);
        if (remoteShare != null) {
            remoteShare.stop(true);
        }
    }

    public void prepareToLeaveVideoInterruption() {
        WmeTrack localVideo = session.getTrack(WmeTrack.Type.LocalVideo, WMEngine.MAIN_VID);
        if (localVideo != null) {
            localVideo.stop(true);
            localVideo.start();
        }
        WmeTrack remoteVideo = session.getTrack(WmeTrack.Type.RemoteVideo, WMEngine.MAIN_VID);
        if (remoteVideo != null) {
            remoteVideo.stop(true);
            remoteVideo.start();
        }
        WmeTrack remoteShare = session.getTrack(WmeTrack.Type.RemoteSharing, WMEngine.MAIN_VID);
        if (remoteShare != null) {
            remoteShare.stop(true);
            remoteShare.start();
        }
    }

}
