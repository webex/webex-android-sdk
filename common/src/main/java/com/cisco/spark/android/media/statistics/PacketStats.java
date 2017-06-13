package com.cisco.spark.android.media.statistics;

public class PacketStats {
    final long audioTx;
    final long audioRx;
    final long videoTx;
    final long videoRx;

    public PacketStats(long audioTx, long audioRx, long videoTx, long videoRx) {
        this.audioTx = audioTx;
        this.audioRx = audioRx;
        this.videoTx = videoTx;
        this.videoRx = videoRx;
    }
}
