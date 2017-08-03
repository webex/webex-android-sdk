package com.cisco.wx2.android.util;

import android.view.MotionEvent;

public class MotionEventBoundaryMapper {

    private float canvasHeight;
    private float canvasWidth;

    public MotionEventBoundaryMapper(float canvasWidth, float canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    private float calculateRatio(float previousValue, float outScopeValue, float referenceValue) {
        return Math.abs((previousValue - referenceValue) / (previousValue - outScopeValue));
    }

    private float calculateEstimateValue(float previousValue, float outScopeValue, float ratio) {
        return ratio * (outScopeValue - previousValue) + previousValue;
    }

    private float getReferenceValue(float outScopeValue, boolean vertical) {
        float targetMinValue = 0;
        float targetMaxValue = vertical ? canvasHeight : canvasWidth;
        return Math.min(targetMaxValue, Math.max(targetMinValue, outScopeValue));
    }

    public void estimateLocationForOutsidePoint(MotionEvent motionEvent, MotionEvent lastValidMotionEvent) {
        boolean isVertical = motionEvent.getY() > canvasHeight || motionEvent.getY() < 0;
        float referenceValueX = getReferenceValue(motionEvent.getX(), isVertical);
        float referenceValueY = getReferenceValue(motionEvent.getY(), isVertical);
        float referenceValue = isVertical ? referenceValueY : referenceValueX;

        float previousValueForCalculatingRatio = isVertical
                ? lastValidMotionEvent.getY()
                : lastValidMotionEvent.getX();
        float outScopeValueForCalculatingRatio = isVertical
                ? motionEvent.getY()
                : motionEvent.getX();

        float previousValueForEstimation = isVertical
                ? lastValidMotionEvent.getX()
                : lastValidMotionEvent.getY();
        float outScopeValueForEstimation = isVertical
                ? motionEvent.getX()
                : motionEvent.getY();

        float ratio = calculateRatio(previousValueForCalculatingRatio, outScopeValueForCalculatingRatio, referenceValue);
        float tempX = isVertical
                ? calculateEstimateValue(previousValueForEstimation, outScopeValueForEstimation, ratio)
                : referenceValue;
        float tempY = !isVertical
                ? calculateEstimateValue(previousValueForEstimation, outScopeValueForEstimation, ratio)
                : referenceValue;

        motionEvent.setLocation(tempX, tempY);
        motionEvent.setAction(MotionEvent.ACTION_UP);
    }

    public boolean outOfScope(MotionEvent substitute) {
        return substitute.getX() > canvasWidth
                || substitute.getX() < 0
                || substitute.getY() > canvasHeight
                || substitute.getY() < 0;
    }
}
