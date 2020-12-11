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

package com.ciscowebex.androidsdk.internal.model;

import java.util.Map;

public class MediaInfoModel {

    private String type;
    private String sdp;
    private boolean videoMuted;
    private boolean audioMuted;
    private boolean dtmfReceiveSupported;
    private String calliopeSupplementaryInformation;
    private LocusMediaDirection direction;
    private Map<String, ReachabilityModel> reachability;

    public MediaInfoModel(String sdp, Map<String, ReachabilityModel> reachability) {
        this(sdp, false, false, reachability);
    }

    public MediaInfoModel(String sdp, boolean audioMuted, boolean videoMuted, Map<String, ReachabilityModel> reachability) {
        this.sdp = sdp;
        this.type = "SDP";
        this.audioMuted = audioMuted;
        this.videoMuted = videoMuted;
        this.reachability = reachability;
    }

    public String getSdp() {
        return sdp;
    }

    public boolean isVideoMuted() {
        return videoMuted;
    }

    public void setVideoMuted(boolean videoMuted) {
        this.videoMuted = videoMuted;
    }

    public boolean isAudioMuted() {
        return audioMuted;
    }

    public void setAudioMuted(boolean audioMuted) {
        this.audioMuted = audioMuted;
    }

    public boolean isDtmfReceiveSupported() {
        return dtmfReceiveSupported;
    }

    public void setDtmfReceiveSupported(boolean dtmfReceiveSupported) {
        this.dtmfReceiveSupported = dtmfReceiveSupported;
    }

    public LocusMediaDirection getDirection() {
        return direction;
    }

    public void setDirection(LocusMediaDirection direction) {
        this.direction = direction;
    }

    public Map<String, ReachabilityModel> getReachability() {
        return reachability;
    }

    public void setReachability(Map<String, ReachabilityModel> reachability) {
        this.reachability = reachability;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "{" +
                "type='" + type + '\'' +
                ", sdp='" + sdp + '\'' +
                ", audioMuted=" + audioMuted  +
                ", videoMuted=" + videoMuted  +
                ", dtmfReceiveSupported=" + dtmfReceiveSupported  +
                ", direction=" + direction  +
                ", calliopeSupplementaryInformation=" + "\"" + calliopeSupplementaryInformation + "\"" +
                ", reachability=" + reachability +
                '}';
    }
}

