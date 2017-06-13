package com.cisco.spark.android.room.audiopairing;

/**
 * This is used as a container to expose the various pairing values from
 * the native audiopairing library
 *
 */
public class UltrasoundMetrics {

    /**
     * Ultrasound noise level
     */
    public float noiseLevel;

    /**
     * Ultrasound signal level
     */
    public float signalLevel;

    /**
     * How many error corrections was done to generate the successful token
     * This is only set on a successful token
     */
    public int errorCorrectionCount;

    /**
     * If a token was successfully decoded this string contains it
     *
     * If no token where found we get an empty string.
     */
    public String token;

    /**
     * If the token is an alto or spark token at the current time.
     * 1 - Alto
     * 0 - Spark
     * Default - Spark
     *
     * Using an int to pass the data from JNI to Java but look into a better type
     */
    public int tokenType;

    public boolean isAltoToken() {
        return tokenType == 1;
    }
}
