package com.cisco.spark.android.room.audiopairing;

import java.nio.FloatBuffer;

public class AudioPairingNative {

    //load shared library
    static {
        System.loadLibrary("audiopairing");
    }

    public static native UltrasoundMetrics checkForToken(FloatBuffer audioData);
    public static native int generateTokenWavFile(String tokenString, String playoutFileName, String pairingSoundFilesPath);
    public static native String encodeProximityToken(String tokenString);
}
