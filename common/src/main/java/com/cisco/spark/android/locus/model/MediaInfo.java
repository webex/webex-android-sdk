package com.cisco.spark.android.locus.model;


import com.cisco.spark.android.roap.model.RoapBaseMessage;

import java.util.Map;

public class MediaInfo {
    private String type;
    private String sdp;
    private boolean videoMuted;
    private boolean audioMuted;
    private boolean dtmfReceiveSupported;
    private String calliopeSupplementaryInformation;
    private MediaDirection direction;
    private Map<String, Object> reachability;
    private RoapBaseMessage roapMessage;

    public MediaInfo(RoapBaseMessage roapMessage) {
        this.roapMessage = roapMessage;
    }

    public MediaInfo(String sdp, String type, String calliopeSupplementaryInformation, Map<String, Object> reachability) {
        this.sdp = sdp;
        this.type = type;
        this.calliopeSupplementaryInformation = calliopeSupplementaryInformation;
        this.reachability = reachability;
    }


    public MediaInfo(String type, String calliopeSupplementaryInformation, Map<String, Object> reachability) {
        this.type = type;
        this.calliopeSupplementaryInformation = calliopeSupplementaryInformation;
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

    public MediaDirection getDirection() {
        return direction;
    }

    public void setDirection(MediaDirection direction) {
        this.direction = direction;
    }

    public Map<String, Object> getReachability() {
        return reachability;
    }

    public void setReachability(Map<String, Object> reachability) {
        this.reachability = reachability;
    }

    public RoapBaseMessage getRoapMessage() {
        return roapMessage;
    }

    public void setRoapMessage(RoapBaseMessage roapMessage) {
        this.roapMessage = roapMessage;
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
