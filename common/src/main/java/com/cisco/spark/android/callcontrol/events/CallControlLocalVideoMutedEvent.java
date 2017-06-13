package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.media.MediaRequestSource;

/**
 * This event is sent when local video is muted by for instance proximity sensor or poor network
 */
public class CallControlLocalVideoMutedEvent {

    private final boolean isMuted;
    private final MediaRequestSource source;
    private final LocusKey locusKey;

    public CallControlLocalVideoMutedEvent(LocusKey locusKey, boolean isMuted, MediaRequestSource source) {
        this.locusKey = locusKey;
        this.isMuted = isMuted;
        this.source = source;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public MediaRequestSource getSource() {
        return source;
    }

}
