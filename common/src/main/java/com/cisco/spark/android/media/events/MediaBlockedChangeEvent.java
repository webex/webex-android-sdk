package com.cisco.spark.android.media.events;

public class MediaBlockedChangeEvent {
    private int mediaId;
    private int videoId;
    private boolean blocked;

    public MediaBlockedChangeEvent(int mediaId, int videoId, boolean blocked) {
        this.mediaId = mediaId;
        this.videoId = videoId;
        this.blocked = blocked;
    }

    public int getMediaId() {
        return mediaId;
    }

    public int getVideoId() {
        return videoId;
    }

    public boolean isBlocked() {
        return blocked;
    }

}
