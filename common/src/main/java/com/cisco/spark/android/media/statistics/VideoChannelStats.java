package com.cisco.spark.android.media.statistics;


public class VideoChannelStats extends ChannelStats {
    private long frameWidth;
    private long frameHeight;
    private long fps;
    private long idr;

    public VideoChannelStats() {
        super();
    }

    public long getFrameWidth() {
        return frameWidth;
    }

    public long getFrameHeight() {
        return frameHeight;
    }

    public long getFps() {
        return fps;
    }

    public long getIdr() {
        return idr;
    }

    public void setVideoChannelStats(long frameWidth, long frameHeight, long fps, long idr) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.fps = fps;
        this.idr = idr;
    }

    public void setFrameWidth(long frameWidth) {
        this.frameWidth = frameWidth;
    }

    public void setFrameHeight(long frameHeight) {
        this.frameHeight = frameHeight;
    }
}
