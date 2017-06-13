package com.cisco.spark.android.media.statistics;


public class VideoStats {
    private VideoChannelStats tx;
    private VideoChannelStats rx;

    public VideoStats() {
        tx = new VideoChannelStats();
        rx = new VideoChannelStats();
    }

    public VideoChannelStats getTx() {
        return tx;
    }

    public void setTx(VideoChannelStats tx) {
        this.tx = tx;
    }

    public VideoChannelStats getRx() {
        return rx;
    }

    public void setRx(VideoChannelStats rx) {
        this.rx = rx;
    }
}
