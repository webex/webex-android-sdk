package com.cisco.spark.android.whiteboard.util;


import android.graphics.Matrix;

public class MatrixBuilder {
    private float scaleFactor;
    private float translateX;
    private float translateY;

    public MatrixBuilder() {
        scaleFactor = 1;
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

    public Matrix build() {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(scaleFactor, scaleFactor);
        matrix.postTranslate(translateX, translateY);
        return matrix;
    }

}
