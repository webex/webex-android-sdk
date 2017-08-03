package com.cisco.spark.android.media.events;

import android.support.annotation.Nullable;

import java.util.Locale;

public class MediaActiveSpeakerVideoMuted extends MediaSessionEvent {

    private final boolean muted;
    private final Long csi;
    private final int vid;

    /**
     * Video was muted for given CSI and videoId
     *
     * @param csi the corresponding csi or null
     * @param vid the videoId which was muted
     */
    public static MediaActiveSpeakerVideoMuted newVideoMuteWithCsiEvent(String callId, @Nullable Long csi, int vid) {
        return new MediaActiveSpeakerVideoMuted(callId, true, csi, vid);
    }

    /**
     * Video was turned on again for given CSI and videoId
     *
     * @param csi the corresponding csi or null
     * @param vid the videoId which was unmuted
     */
    public static MediaActiveSpeakerVideoMuted newVideoOnWithCsiEvent(String callId, @Nullable Long csi, int vid) {
        return new MediaActiveSpeakerVideoMuted(callId, false, csi, vid);
    }

    private MediaActiveSpeakerVideoMuted(String callId, boolean muted, @Nullable Long csi, int vid) {
        super(callId);
        this.muted = muted;
        this.csi = csi;
        this.vid = vid;
    }

    public boolean isMuted() {
        return muted;
    }

    public Long getCsi() {
        return csi;
    }

    public boolean hasCsi() {
        return csi != null;
    }

    public int getVideoId() {
        return vid;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "MediaActiveSpeakerVideoMuted muted=%s csi=%d vid=%d", muted, csi, vid);
    }

}
