package com.cisco.spark.android.callcontrol.events;

import android.net.Uri;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlViewWhiteboardShare {
    private final LocusKey locusKey;
    private final Uri mediaShareUrl;

    public CallControlViewWhiteboardShare(LocusKey locusKey, Uri mediaShareUrl) {
        this.locusKey = locusKey;
        this.mediaShareUrl = mediaShareUrl;
    }

    public Uri getMediaShareUrl() {
        return mediaShareUrl;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
