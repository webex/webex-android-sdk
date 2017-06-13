package com.cisco.spark.android.whiteboard.util;


import android.graphics.Matrix;
import android.graphics.PointF;

public class MatrixBuilder {
    private float scaleFactor;
    private float translateX;
    private float translateY;
    private PointF focalPoint;

    public MatrixBuilder() {
        scaleFactor = 1;
        focalPoint = new PointF(0, 0);
    }

    public MatrixBuilder setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public MatrixBuilder setTranslateX(float translateX) {
        this.translateX = translateX;
        return this;
    }

    public MatrixBuilder setTranslateY(float translateY) {
        this.translateY = translateY;
        return this;
    }

    public MatrixBuilder setFocalPoint(PointF focalPoint) {
        this.focalPoint = focalPoint;
        return this;
    }

    public Matrix build() {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(scaleFactor, scaleFactor, focalPoint.x, focalPoint.y);
        matrix.postTranslate(translateX , translateY);
        return matrix;
    }

}
