package com.cisco.spark.android.lyra.model;

import android.net.Uri;

public class LyraSpaceSession {
    private Uri locusUrl;
    private boolean media;
    private boolean share;

    public LyraSpaceSession(Uri locusUrl,
                            boolean media,
                            boolean share) {
        this.locusUrl = locusUrl;
        this.media = media;
        this.share = share;
    }

    public Uri getLocusUrl() {
        return locusUrl;
    }

    public boolean hasMedia() {
        return media;
    }

    public boolean hasShare() {
        return share;
    }
}
