package com.cisco.spark.android.whiteboard.renderer;

import android.graphics.Color;
import android.view.MotionEvent;

import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.wacom.ink.rasterization.BlendMode;

import java.util.ArrayList;
import java.util.List;

public class WhiteboardLocalWriter {

    public enum DrawMode { NORMAL, ERASE }
    private DrawMode drawMode = DrawMode.NORMAL;

    private int drawColor;

    private WhiteboardRenderer renderer;

    private float[] previousX = new float[WhiteboardConstants.MAX_TOUCH_POINTERS_PLENTY];
    private float[] previousY = new float[WhiteboardConstants.MAX_TOUCH_POINTERS_PLENTY];

    public WhiteboardLocalWriter(WhiteboardRenderer renderer) {

        this.renderer = renderer;

        drawMode = DrawMode.NORMAL;
        drawColor = Color.BLACK;

        for (int i = 0; i < WhiteboardConstants.maxTouchPointers; i++) {
            previousX[i] = -1;
            previousY[i] = -1;
        }
    }

    public List<Pointer> renderLocalInputs(MotionEvent motionEvent, boolean drawHistoricalPoints) {
        List<Pointer> pointersChanged = new ArrayList<>();
        BlendMode strokeBlendMode = isInEraseMode() ? BlendMode.BLENDMODE_ERASE : BlendMode.BLENDMODE_NORMAL;

        for (int pointerIndex = 0; pointerIndex < motionEvent.getPointerCount(); pointerIndex++) {
            int pointerId = motionEvent.getPointerId(pointerIndex);
            if (pointerId >= WhiteboardConstants.maxTouchPointers) {
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
                    writer = renderer.createNewLocalWILLWriter(drawColor, strokeBlendMode);
                    if (writer == null) { //The board was cleared while drawing
                        return pointersChanged;
                    }
                    renderer.getLocalWriters().put(pointerId, writer);
                } else {
                    writer = renderer.getLocalWriter(pointerId);
                    if (writer == null) { //The board was cleared while drawing
                        return pointersChanged;
                    }
                    if (drawHistoricalPoints) {
                        writer.drawHistoricalStrokes(pointerIndex, motionEvent);
                    }
                }

                pointersChanged.add(new Pointer(writer, isMoveEvent ? MotionEvent.ACTION_MOVE : motionEvent.getActionMasked(), x, y));

                boolean strokeEnded = writer.buildPath(pointerIndex, motionEvent);
                writer.drawPoints(isMoveEvent, strokeEnded);
                if (strokeEnded) {
                    renderer.moveWriterToBeingSaved(writer, pointerId);
                    previousX[pointerId] = -1;
                    previousY[pointerId] = -1;
                } else {
                    previousX[pointerId] = x;
                    previousY[pointerId] = y;
                }
            }
        }
        return pointersChanged;
    }

    void finishLocalInputs() {
        boolean hadLocalWriters = false;
        for (int pointerId = 0; pointerId < WhiteboardConstants.maxTouchPointers; pointerId++) {
            LocalWILLWriter writer = renderer.getLocalWriter(pointerId);
            if (writer == null) {
                continue;
            }
            if (!writer.canBePersisted()) {
                renderer.removeLocalWriter(pointerId);
                continue;
            }
            writer.prepareForPersistence();
            renderer.moveWriterToBeingSaved(writer, pointerId);
            hadLocalWriters = true;
        }
        if (hadLocalWriters) {
            renderer.persistFinishedStrokes();
        }
    }


    private boolean pointerHasChanged(int pointerId, float currentX, float currentY) {
        return previousX[pointerId] != currentX || previousY[pointerId] != currentY;
    }

    public void clearWhiteboardLocal() {
        renderer.clearWhiteboardLocal();
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
