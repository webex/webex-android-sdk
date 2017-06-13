package com.cisco.spark.android.util;

import android.support.annotation.NonNull;
import android.view.animation.Interpolator;

import java.util.Arrays;
import java.util.Comparator;

public class AppScaleInterpolator implements Interpolator, Comparator<AppScaleInterpolator.ControlPoint> {

    ControlPoint[] controlPoints;

    public AppScaleInterpolator(ControlPoint... controlPoints) {
        this.controlPoints = controlPoints;
        Arrays.sort(this.controlPoints, this);
    }

    @Override
    public float getInterpolation(float x) {

        if (controlPoints == null) {
            controlPoints = new ControlPoint[] {
                new ControlPoint(0.0f, 0.0f), new ControlPoint(1.0f, 1.0f)
            };
        }

        ControlPoint upper = null;
        ControlPoint lower = null;

        for (int i = 0; i < controlPoints.length - 1; i++) {
            if (x > controlPoints[i].x && x <= controlPoints[i + 1].x) {
                upper = controlPoints[i + 1];
                lower = controlPoints[i];
            }
        }

        if (upper == null || lower == null)
            return 1.0f;

        return accelerateDecelerate(x, upper.x, lower.x, lower.y, upper.y);
    }

    private float accelerateDecelerate(float x, float upperX, float lowerX, float lowerY, float upperY) {
        float v = 1 / Math.abs(upperX - lowerX);
        double offset = upperY < lowerY ? Math.PI : 0;
        double output = (1 - Math.cos((x - lowerX) * Math.PI * v + offset)) / 2;
        return (float) (output * Math.abs(upperY - lowerY) + Math.min(upperY, lowerY));
    }

    @Override
    public int compare(ControlPoint lhs, ControlPoint rhs) {
        if (lhs.x < rhs.x)
            return -1;
        else if (lhs.x > rhs.x)
            return 1;
        else
            return 0;
    }

    public static class ControlPoint implements Comparable<ControlPoint> {

        private float x;
        private float y;

        public ControlPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(@NonNull ControlPoint other) {
            if (this.x < other.x)
                return -1;
            else if (this.x > other.x)
                return 1;
            else
                return 0;
        }
    }
}
