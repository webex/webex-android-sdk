package com.cisco.spark.android.media.statistics;


public class ChannelStats {
    private long bytes;
    private long packets;
    private float lossRatio;
    private long jitter;
    private long rtt;        // only TX value is valid
    private long bitrate;

    public ChannelStats() {
    }

    public long getBytes() {
        return bytes;
    }

    public long getPackets() {
        return packets;
    }

    public float getLossRatio() {
        return lossRatio;
    }

    public long getJitter() {
        return jitter;
    }

    public long getRtt() {
        return rtt;
    }

    public long getBitrate() {
        return bitrate;
    }

    public void setChannelStats(long bytes, long packets, float lossRatio, long jitter, long rtt, long bitrate) {
        this.bytes = bytes;
        this.packets = packets;
        this.lossRatio = lossRatio;
        this.jitter = jitter;
        this.rtt = rtt;
        this.bitrate = bitrate;
    }

    public void setRtt(long rtt) {
        this.rtt = rtt;
    }

    public void setBitrate(long bitrate) {
        this.bitrate = bitrate;
    }
}
