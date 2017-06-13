package com.cisco.spark.android.room;

public class MockProximityDetector implements ProximityDetector {

    @Override
    public void stopListening(TokenListener listener, boolean switchToWme) {
    }

    @Override
    public void stopListeningBlocking(TokenListener tokenListener, boolean switchToWme) {
    }

    @Override
    public void startListening(TokenListener listener, boolean switchFromWme) {
    }

    @Override
    public boolean isListening() {
        return false;
    }

    @Override
    public void resetState() {
    }

    @Override
    public void pauseListeningFor(TokenListener listener, int secondsToNextTokenEmit) {
    }

    @Override
    public void mediaEngineAudioTrackReady() {
    }

}
