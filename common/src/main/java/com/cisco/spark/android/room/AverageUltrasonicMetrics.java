package com.cisco.spark.android.room;

import com.cisco.spark.android.room.audiopairing.UltrasoundMetrics;

public class AverageUltrasonicMetrics {

    // These values are based on statistical records, and is close to the lowest values of signal and noise
    // at which we will be able to pick up a token.  We will most likely have to tune these
    public static final float THRESHOLD_SIGNAL = 0.000020f;
    public static final float THRESHOLD_NOISE = 0.00005f;

    // Calculate average over a number of metrics, we get about 20 a second
    // Probably need tuning.  A low number means more calculations, a high number will miss
    // opportunities as we will detect the token before we think we are in range
    public static final int NUMBER_OF_METRICS = 20;

    private int metricsCount;
    private UltrasoundMetrics ultrasoundMetrics;

    public enum AverageResult {
        IN_RANGE,
        OUT_OF_RANGE,
        NOT_PROCESSED
    }

    public AverageUltrasonicMetrics() {
        ultrasoundMetrics = new UltrasoundMetrics();
        metricsCount = 0;
    }

    public void add(UltrasoundMetrics addMetrics) {
        ultrasoundMetrics.noiseLevel += addMetrics.noiseLevel;
        ultrasoundMetrics.signalLevel += addMetrics.signalLevel;
        metricsCount++;
    }

    // We get too many metric values to do division on all of them
    // Do the division only after having collected N number of values
    public AverageResult process() {
        if (metricsCount == NUMBER_OF_METRICS) {

            float averageNoise = ultrasoundMetrics.noiseLevel / metricsCount;
            float averageSignal = ultrasoundMetrics.signalLevel / metricsCount;

            boolean inRange = (averageSignal >= THRESHOLD_SIGNAL && averageNoise >= THRESHOLD_NOISE);

            metricsCount = 0;
            ultrasoundMetrics.noiseLevel = 0;
            ultrasoundMetrics.signalLevel = 0;

            return inRange ? AverageResult.IN_RANGE : AverageResult.OUT_OF_RANGE;
        }
        return AverageResult.NOT_PROCESSED;
    }

}
