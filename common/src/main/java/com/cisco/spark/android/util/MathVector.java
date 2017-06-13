package com.cisco.spark.android.util;

import android.graphics.PointF;

public class MathVector {
    public float x;
    public float y;

    public MathVector(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static MathVector createByPoints(PointF start, PointF end) {
        return new MathVector(end.x - start.x, end.y - start.y);
    }

    public static double product(MathVector mv1, MathVector mv2) {
        return mv1.x * mv2.x + mv1.y * mv2.y;
    }

    public double module() {
        return Math.sqrt(x * x + y * y);
    }

    public static double calculateAngle(MathVector mv1, MathVector mv2) {
        double productValue = product(mv1, mv2);
        double moduleMv1 = mv1.module();
        double moduleMv2 = mv2.module();
        double cosValue = productValue / (moduleMv1 * moduleMv2);
        if (cosValue < -1 && cosValue > -2) {
            cosValue = -1;
        } else if (cosValue > 1 && cosValue < 2) {
            cosValue = 1;
        }
        return cosValue;
    }
}
