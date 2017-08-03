package com.cisco.spark.android.lyra.model;

public class LyraSpaceSessionSupported {

    public enum Media {
        NONE, AUDIO, AUDIOVIDEO
    }

    private final Media media;

    public LyraSpaceSessionSupported(Media media) {
        this.media = media;
    }

    public Media getMedia() {
        return media;
    }
}
