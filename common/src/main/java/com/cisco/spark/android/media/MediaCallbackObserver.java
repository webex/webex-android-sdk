package com.cisco.spark.android.media;

public interface MediaCallbackObserver {
    public void onSDPReady(MediaSession mediaSession, String sdp);
    public void onVideoBlocked(boolean blocked);
}
