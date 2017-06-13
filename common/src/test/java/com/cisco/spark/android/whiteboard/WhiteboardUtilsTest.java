package com.cisco.spark.android.whiteboard;


import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class WhiteboardUtilsTest {

    @Test
    public void testScaleRawOutputPoints() {
        float[] points = new float [] { 1.2345678f, 2.34566789f, 3.4567890f };
        float[] scaledPoints = WhiteboardUtils.scaleRawOutputPoints(points, 1.0f);
        assertEquals(1.234000, scaledPoints[0], 0.0000001);
        assertEquals(2.345000, scaledPoints[1], 0.0000001);
        assertEquals(3.456000, scaledPoints[2], 0.0000001);


        float[] points2 = new float [] { 2, 4, 6 };
        float[] scaledPoints2 = WhiteboardUtils.scaleRawOutputPoints(points2, 2f);
        assertEquals(1.0f, scaledPoints2[0], 0.0000001);
        assertEquals(2.0f, scaledPoints2[1], 0.0000001);
        assertEquals(3.0f, scaledPoints2[2], 0.0000001);
    }
}
