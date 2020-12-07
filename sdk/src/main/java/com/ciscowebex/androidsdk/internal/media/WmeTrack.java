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

package com.ciscowebex.androidsdk.internal.media;

import android.view.View;
import com.ciscowebex.androidsdk.utils.Json;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.MediaConnection;
import com.webex.wme.MediaTrack;
import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

public class WmeTrack {

    public enum Type {
        LocalAudio, RemoteAudio, LocalSharing, RemoteSharing,
        LocalVideo, RemoteVideo, AuxVideo, Preview
    }

    @StringPart
    private final WmeTrack.Type type;

    private MediaTrack track;

    @StringPart
    private long vid = -1;

    @StringPart
    private MediaConnection.MediaDirection remoteSDPDirection = MediaConnection.MediaDirection.Inactive;

    private View renderView;

    @StringPart
    private boolean receiving = true;

    @StringPart
    private boolean sending = true;

    private int videoHeight = 0;

    private int videoWidth = 0;

    private int port;

    private String ipAddress;

    @StringPart
    private boolean ready;

    @StringPart
    private boolean viewAdded;

    public WmeTrack(Type type) {
        this.type = type;
        if (type == Type.LocalSharing || type == Type.AuxVideo) {
            this.sending = false;
        }
    }

    public void init(MediaTrack track) {
        Ln.d("Track init: " + this + " with " + track);
        this.track = track;
        this.ready = true;
    }

    public void start() {
        if (!isReady()) {
            Ln.d("Track is not ready, " + this);
            return;
        }
        if (isLocalTrack()) {
            if (type != Type.Preview && !isSdpAllowSending()) {
                Ln.d("Skipped - not sending media, " + this);
                return;
            }
        }
        else {
            if (type != Type.AuxVideo && !isSdpAllowReceiving()) {
                Ln.d("Skipped - not receiving media, " + this);
                return;
            }
        }
        if (type == Type.LocalAudio || type == Type.RemoteAudio) {
            if (port == 0) {
                Ln.d("Skipped - port is 0, " + this);
                return;
            }
        }
        else {
            attachView(getRenderView());
            if (type == Type.LocalVideo || type == Type.Preview) {
                track.SetRenderMode(MediaTrack.ScalingMode.CropFill);
            }
        }
        if (isLocalTrack()) {
            track.Start(!isSending());
        }
        else {
            track.Start(!isReceiving());
        }
        Ln.d("Track start: " + this);
    }

    public void stop(boolean removeView) {
        if (!isReady()) {
            Ln.d("Track is not ready, " + this);
            return;
        }
        //track.Stop();
        if (removeView && (type == Type.LocalVideo || type == Type.RemoteVideo
                || type == Type.AuxVideo || type == Type.Preview
                || type == Type.LocalSharing || type == Type.RemoteSharing)) {
            detachView(getRenderView());
        }
        Ln.d("Track stopped: " + this);
    }

    public void release() {
        setReady(false);
        detachView(getRenderView());
        if (this.track != null) {
            this.track.destroy();
            this.track = null;
        }
        Ln.d("Track released: " + this);
    }

    public void clear() {
        renderView = null;
        vid = -1;
        videoWidth = 0;
        videoHeight = 0;
        port = 0;
        ipAddress = "";
        remoteSDPDirection = MediaConnection.MediaDirection.Inactive;
        receiving = true;
        sending = true;
        if (type == Type.LocalSharing || type == Type.AuxVideo) {
            this.sending = false;
        }
    }

    public void updateRenderMode() {
        MediaTrack track = getTrack();
        View view = getRenderView();
        if (track != null && view != null) {
            track.SetRenderMode(MediaHelper.getRenderMode(track, view));
        }
    }

    public Type getType() {
        return type;
    }

    public MediaTrack getTrack() {
        return track;
    }

    public long getVid() {
        return vid;
    }

    public void setVid(long vid) {
        this.vid = vid;
    }

    public void setRemoteSDPDirection(MediaConnection.MediaDirection remoteSDPDirection) {
        this.remoteSDPDirection = remoteSDPDirection;
    }

    public View getRenderView() {
        return renderView;
    }

    public void addView(View view) {
        if (view != null && renderView != view) {
            renderView = view;
            if (type == Type.AuxVideo && isReady() && track != null) {
                attachView(view);
            }
        }
    }

    public void removeView(View view) {
        if (view != null && renderView == view) {
            renderView = null;
            if (type == Type.AuxVideo && isReady() && track != null) {
                detachView(view);
            }
        }
    }

    public void removeAllViews() {
        renderView = null;
    }

    public boolean isReceiving() {
        return receiving;
    }

    public void setReceiving(boolean receiving) {
        this.receiving = receiving;
    }

    public boolean isSending() {
        return sending;
    }

    public void setSending(boolean sending) {
        this.sending = sending;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isLocalTrack() {
        return type == Type.LocalAudio || type == Type.LocalVideo || type == Type.LocalSharing || type == Type.Preview;
    }

    public boolean isSdpAllowSending() {
        return remoteSDPDirection != MediaConnection.MediaDirection.Inactive && remoteSDPDirection != MediaConnection.MediaDirection.SendOnly;
    }

    public boolean isSdpAllowReceiving() {
        return remoteSDPDirection != MediaConnection.MediaDirection.Inactive && remoteSDPDirection != MediaConnection.MediaDirection.RecvOnly;
    }

    private void attachView(View view) {
        if (view != null && !viewAdded) {
            if (track.addRenderWindow(view) == 0) {
                viewAdded = true;
            }
        }
    }

    private void detachView(View view) {
        if (view != null && viewAdded) {
            if (track.removeRenderWindow(view) == 0) {
                viewAdded = false;
            }
        }
    }

    public String debugVideo() {
        if (track != null && (type == Type.LocalVideo || type == Type.RemoteVideo)) {
            return Json.get().toJson(track.getVideoTrackStatistics());
        }
        return "";
    }

    @Override
    public String toString() {
        return Objects.toStringByAnnotation(this, "WmeTrack[" + System.identityHashCode(this) + "]");
    }
}
