package com.cisco.spark.android.room;

/**
 * Detect if the device is in proximity of a Spark registered video system
 */
public interface ProximityDetector {

    /**
     * Stop listening thread by flagging it to stop at next iteration
     * @param listener
     * @param switchToWme
     */
    void stopListening(TokenListener listener, boolean switchToWme);

    /**
     * Stop listening by interrupting the recording by stopping and releasing
     * the recorder while recording
     *
     * Will also block waiting for the resources to be released so shouldn't be called on the main
     * thread
     *
     * @param tokenListener
     * @param switchToWme
     */
    void stopListeningBlocking(TokenListener tokenListener, boolean switchToWme);

    void startListening(TokenListener listener, boolean switchFromWme);

    boolean isListening();

    /**
     * If the user turns off the proximity features while in proximity
     * we want to clear the last token heard
     */
    void resetState();

    /**
     * We are in proximity of a room, we know when the token will change, so
     * release the microphone until we need to listen for the new token
     *
     * This saves battery, and provides useful metrics to how long it takes
     * to get a token when we actually expect there to be one.
     *
     * @param secondsToNextTokenEmit when the token producer will change the active token
     */
    void pauseListeningFor(TokenListener listener, int secondsToNextTokenEmit);

    /**
     * Callback for when media engine audio track is ready
     */
    void mediaEngineAudioTrackReady();

}
