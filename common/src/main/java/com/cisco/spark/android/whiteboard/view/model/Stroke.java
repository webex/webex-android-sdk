package com.cisco.spark.android.whiteboard.view.model;

import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.wacom.ink.rasterization.BlendMode;
import com.wacom.ink.utils.Utils;

import java.nio.FloatBuffer;
import java.util.Arrays;

public class Stroke {

    private final float[] points;
    private final int color;
    private final int stride;
    private final BlendMode blendMode;

    public Stroke(float[] points, int color, int stride, BlendMode blendMode) {
        this.points = points;
        this.color = color;
        this.stride = stride;
        this.blendMode = blendMode;
    }

    public float[] getPoints() {
        return points;
    }

    public FloatBuffer getScaledPointsFloatBuffer(float scaleFactor) {
        FloatBuffer wrappedPoints = FloatBuffer.wrap(WhiteboardUtils.scaleRawInputPoints(points, scaleFactor));
        return Utils.createNativeFloatBuffer(wrappedPoints, 0, points.length);
    }

    public int getColor() {
        return color;
    }

    public int getStride() {
        return stride;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public int getSize() {
        return points.length;
    }

    @Override
    public String toString() {
        return "Stroke{" +
               "points=" + Arrays.toString(points) +
               ", color=" + color +
               ", stride=" + stride +
               ", blendMode=" + blendMode +
               '}';
    }
}
