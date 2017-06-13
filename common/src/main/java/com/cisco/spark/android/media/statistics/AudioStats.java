package com.cisco.spark.android.media.statistics;


public class AudioStats {
    private ChannelStats tx;
    private ChannelStats rx;

    public AudioStats() {
        tx = new ChannelStats();
        rx = new ChannelStats();
    }

    public ChannelStats getTx() {
        return tx;
    }

    public void setTx(ChannelStats tx) {
        this.tx = tx;
    }

    public ChannelStats getRx() {
        return rx;
    }

    public void setRx(ChannelStats rx) {
        this.rx = rx;
    }
}
