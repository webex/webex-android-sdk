package com.cisco.spark.android.callcontrol.events;

import android.net.Uri;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlEndWhiteboardShare {

    private final LocusKey locusKey;
    private final Uri mediaShareUrl;

    public CallControlEndWhiteboardShare(LocusKey locusKey, Uri mediaShareUrl) {
        this.locusKey = locusKey;
        this.mediaShareUrl = mediaShareUrl;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
    public Uri getMediaShareUrl() {
        return mediaShareUrl;
    }
}
