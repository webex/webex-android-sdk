package com.cisco.spark.android.room.audiopairing;

import java.nio.FloatBuffer;

/**
 * TODO: Unfinished!  Pretends that sampleSizeInBits is always 16.
 */
public final class PcmUtils {

    private PcmUtils() {
    }

    public static void toFloatData(final byte[] byteData, int length, final int sampleSizeInBits, FloatBuffer data) {
        final float scale = (1 << (sampleSizeInBits - 1)) - 1;
        int idx = 0;
        for (int q = 0; q < length; q++) {
            int value = byteData[idx++] & 0xff;
            value |= byteData[idx++] << 8;
            data.put(q, value / scale);
        }
    }

    public static void toFloatData(final short[] shortData, int shortDataLength, final int sampleSizeInBits, FloatBuffer data) {
        final float scale = (1 << (sampleSizeInBits - 1)) - 1;
        for (int q = 0; q < shortDataLength; q++) {
            data.put(q, shortData[q] / scale);
        }
    }

}
