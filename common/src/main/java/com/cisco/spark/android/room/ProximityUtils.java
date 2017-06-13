package com.cisco.spark.android.room;

import com.cisco.spark.android.util.Strings;

public class ProximityUtils {

    public static String extractSystem(String hexEncoded) {
        String[] segments = new String[4];
        for (int i = 0; i < 4; i++) {
            final int start = i * 2;
            int segment = extractSegment(hexEncoded, start, (start + 2));
            segments[i] = String.valueOf(segment);
        }
        return Strings.join(".", segments);
    }

    public static int extractSegment(String originalString, int start, int end) {
        final String substring = originalString.substring(start, end);
        final Integer intValue = Integer.valueOf(substring, 16);
        return intValue;
    }

}
