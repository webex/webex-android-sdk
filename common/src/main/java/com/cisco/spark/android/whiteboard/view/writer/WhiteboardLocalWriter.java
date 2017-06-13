package com.cisco.spark.android.whiteboard.view.writer;

import android.graphics.Color;
import android.view.MotionEvent;

import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.view.WhiteboardController;
import com.wacom.ink.rasterization.BlendMode;

import java.util.ArrayList;
import java.util.List;

public class WhiteboardLocalWriter {

    private final static float ORIGINAL_SCALE_FACTOR = 1;

    public enum DrawMode { NORMAL, ERASE }
    private DrawMode drawMode = DrawMode.NORMAL;

    private int drawColor;

    private WhiteboardController whiteboardController;

    private float[] previousX = new float[WhiteboardConstants.MAX_TOUCH_POINTERS];
    private float[] previousY = new float[WhiteboardConstants.MAX_TOUCH_POINTERS];
    private List<Pointer> pointersChanged = new ArrayList<>();

    public WhiteboardLocalWriter(WhiteboardController whiteboardController) {

        this.whiteboardController = whiteboardController;

        drawMode = DrawMode.NORMAL;
        drawColor = Color.BLACK;

        for (int i  = 0; i < WhiteboardConstants.MAX_TOUCH_POINTERS; i++) {
            previousX[i] = -1;
            previousY[i] = -1;
        }
    }

    public void renderLocalInputs(MotionEvent motionEvent) {
        renderLocalInputs(motionEvent, ORIGINAL_SCALE_FACTOR, true);
    }

    public void renderLocalInputs(MotionEvent motionEvent, boolean renderHistoricalPoints) {
        renderLocalInputs(motionEvent, ORIGINAL_SCALE_FACTOR, renderHistoricalPoints);
    }

    public void renderLocalInputs(MotionEvent motionEvent, float scaleFactor, boolean renderHistoricalPoints) {
        pointersChanged.clear();
        BlendMode strokeBlendMode = isInEraseMode() ? BlendMode.BLENDMODE_ERASE : BlendMode.BLENDMODE_NORMAL;

        for (int pointerIndex = 0; pointerIndex < motionEvent.getPointerCount(); pointerIndex++) {
            int pointerId = motionEvent.getPointerId(pointerIndex);
            if (pointerId >= WhiteboardConstants.MAX_TOUCH_POINTERS) {
                continue;
            }

            float x = motionEvent.getX(pointerIndex);
            float y = motionEvent.getY(pointerIndex);
            boolean pointerIdBelongsToAction = motionEvent.getActionIndex() == pointerIndex;
            boolean isMoveEvent = motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE || !pointerIdBelongsToAction;
            if (!isMoveEvent || pointerHasChanged(pointerId, x, y)) {
                LocalWILLWriter writer;
                if (pointerIdBelongsToAction &&
                        (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN || motionEvent.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)) {
                    writer = whiteboardController.createNewLocalWILLWriter(drawColor, strokeBlendMode, scaleFactor);
                    whiteboardController.getLocalWriters().put(pointerId, writer);
                } else {
                    writer = whiteboardController.getLocalWriter(pointerId);
                    if (writer == null) { //The board was cleared while drawing
                        return;
                    }

                    if (renderHistoricalPoints) {
                        writer.drawHistoricalStrokes(pointerIndex, motionEvent);
                    }
                }
                if (writer == null) return;
                pointersChanged.add(new Pointer(writer, isMoveEvent ? MotionEvent.ACTION_MOVE : motionEvent.getActionMasked(), x, y));

                boolean strokeEnded = writer.buildPath(pointerIndex, motionEvent);
                writer.drawPoints(isMoveEvent, strokeEnded);
                if (strokeEnded) {
                    whiteboardController.moveWriterToBeingSaved(writer, pointerId);
                    writer.dispose();
                    previousX[pointerId] = -1;
                    previousY[pointerId] = -1;
                } else {
                    previousX[pointerId] = x;
                    previousY[pointerId] = y;
                }
            }
        }
    }

    private boolean pointerHasChanged(int pointerId, float currentX, float currentY) {
        return previousX[pointerId] != currentX || previousY[pointerId] != currentY;
    }

    public void clearWhiteboardLocal() {
        whiteboardController.clearWhiteboardLocal();
        setDrawMode(DrawMode.NORMAL);
    }

    public void selectColor(int color) {
        drawColor = color;
        setDrawMode(DrawMode.NORMAL);
    }

    public void selectEraser() {
        setDrawMode(DrawMode.ERASE);
    }

    public DrawMode getDrawMode() {
        return drawMode;
    }

    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
    }

    public boolean isInEraseMode() {
        return drawMode.equals(DrawMode.ERASE);
    }

    public List<Pointer> getPointersChanged() {
        return pointersChanged;
    }

    public void clearPointersChanged() {
        pointersChanged.clear();
    }

    public static class Pointer {
        public LocalWILLWriter writer;
        public int action;
        public float x;
        public float y;

        public Pointer(LocalWILLWriter writer, int action, float x, float y) {
            this.writer = writer;
            this.action = action;
            this.x = x;
            this.y = y;
        }
    }
}
