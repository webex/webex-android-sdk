package com.cisco.spark.android.media;

public final class MediaSessionUtils {

    private MediaSessionUtils() {
    }

    public static String toString(MediaSession mediaSession) {
        return String.format("mediaSession = %s%s",
                mediaSession == null ? null : mediaSession,
                mediaSession == null ? "" : " (" + ("mediaSessionStarted ? " + mediaSession.isMediaSessionStarted() + " mediaStarted ? " + mediaSession.isMediaStarted() + " isMediaSessionEnding ? " + mediaSession.isMediaSessionEnding() + ")"));
    }

}
