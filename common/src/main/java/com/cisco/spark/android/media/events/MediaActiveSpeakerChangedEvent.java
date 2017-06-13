package com.cisco.spark.android.media.events;

import com.cisco.spark.android.media.MediaSessionEngine;

import java.util.Locale;

public class MediaActiveSpeakerChangedEvent {

    private final long mid;
    private final long vid;
    private long[] oldCSIs;
    private long[] newCSIs;

    public MediaActiveSpeakerChangedEvent(long mid, long vid, long[] oldCSIs, long[] newCSIs) {
        this.mid = mid;
        this.vid = vid;
        this.oldCSIs = oldCSIs;
        this.newCSIs = newCSIs;
    }

    public long getMediaId() {
        return mid;
    }

    public long getVideoId() {
        return vid;
    }

    public long[] getOldCSIs() {
        return oldCSIs;
    }

    public long[] getNewCSIs() {
        return newCSIs;
    }

    public boolean isMainVideo() {
        return getVideoId() == 0;
    }

    public boolean videoChanged() {
        return getMediaId() == MediaSessionEngine.VIDEO_MID;
    }

    public boolean mainVideoChanged() {
        return isMainVideo() && videoChanged();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "MediaActiveSpeakerChangedEvent mid = %d vid = %d", mid, vid);
    }

}
