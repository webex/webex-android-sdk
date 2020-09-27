package com.ciscowebex.androidsdk.internal.model;

public class WMEIceListModel {
    private WMEIceModel audio;
    private WMEIceModel video;
    private WMEIceModel share;

    public WMEIceModel getAudio() {
        return audio;
    }

    public void setAudio(WMEIceModel audio) {
        this.audio = audio;
    }

    public WMEIceModel getVideo() {
        return video;
    }

    public void setVideo(WMEIceModel video) {
        this.video = video;
    }

    public WMEIceModel getShare() {
        return share;
    }

    public void setShare(WMEIceModel share) {
        this.share = share;
    }
}
