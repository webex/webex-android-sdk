package com.cisco.spark.android.media.events;

public class AvailableMediaChangeEvent {
    private int mediaId;
    private int count;
    public AvailableMediaChangeEvent(int mediaId, int count) {
        this.mediaId = mediaId;
        this.count = count;
    }
    public int getCount() {
        return count;
    }

    public int getMediaId() {
        return mediaId;
    }

}
