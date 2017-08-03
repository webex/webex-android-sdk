package com.cisco.spark.android.media;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.webex.wme.MediaSessionAPI;
import com.webex.wme.TraceServerSink;

/**
 * Adapter to avoid adding empty methods to the mock implementation
 */
public class MediaEngineAdapter implements MediaEngine {

    @Override
    public void initialize() {

    }

    @Override
    public void uninitialize() {

    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public MediaSession createMediaSession(String callId) {
        return null;
    }

    @Override
    public void setActiveMediaSession(MediaSession mediaSession) {

    }

    @Override
    public MediaSession getActiveMediaSession() {
        return null;
    }

    @Override
    public void setDeviceSettings(String deviceSettings) {

    }

    @Override
    public void setLoggingLevel(MediaSessionAPI.TraceLevelMask level) {

    }

    @Override
    @Nullable
    public Bitmap getLastContentFrame() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void setAudioDataListener(AudioDataListener listener) {

    }

    @Override
    public void clearAudioDataListener(AudioDataListener listener) {

    }

    @Override
    public void headsetPluggedIn() {

    }

    @Override
    public void headsetPluggedOut() {

    }

    @Override
    public void setTraceServerSink(TraceServerSink serverSink) {

    }

    @Override
    public void startTraceServer(String clusters) {

    }

    @Override
    public boolean isLastTraceServerUsable() {
        return false;
    }
}
