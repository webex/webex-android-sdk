package com.cisco.spark.android.media;

import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

public class MockMediaSession extends MediaSessionAdapter {

    private final NaturalLog ln;
    // FIXME: doesn't seem to be needed see usage
    private boolean mediaStarted;
    private boolean mediaSessionStarted;
    private boolean isScreenSharing = false;

    public MockMediaSession(Ln.Context lnContext) {
        ln = Ln.get(lnContext, "MockMediaSession");
    }

    @Override
    public void startSession(String deviceSettings, MediaEngine.MediaDirection mediaDirection, MediaCallbackObserver mediaCallbackObserver) {
        ln.i("startSession()");
        mediaSessionStarted = true;
    }

    @Override
    public void endSession() {
        mediaSessionStarted = false;
    }

    @Override
    public boolean isMediaSessionEnding() {
        return mediaSessionStarted;
    }

    @Override
    public boolean isMediaSessionStarted() {
        return mediaSessionStarted;
    }

    @Override
    public String getLocalSdp() {
        // Overriding since default adapter implementation is to return null
        return "";
    }

    @Override
    public void startMedia() {
        ln.i("startMedia()");
        mediaStarted = true;
    }

    // FIXME: Should this set this.mediaStarted?
    @Override
    public void stopMedia() {
        ln.i("stopMedia()");
    }

    // FIXME: Should this do something to this.mediaStarted?
    @Override
    public void restartMedia() {
        ln.i("restartMedia()");
    }

    // FIXME: Should this report this.mediaStarted?
    @Override
    public boolean isMediaStarted() {
        return false;
    }

    @Override
    public int getMaxStreamCount() {
        return 1;
    }

    @Override
    public void startScreenShare(ScreenShareCallback screenShareCallback, String startScreenShare) {
        isScreenSharing = true;
    }

    @Override
    public void stopScreenShare(String shareId) {
        isScreenSharing = false;
    }

    @Override
    public boolean isScreenSharing() {
        return isScreenSharing;
    }

}
