package com.cisco.spark.android.whiteboard.renderer;

import android.graphics.Color;

import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.wacom.ink.rasterization.BlendMode;
import com.wacom.ink.rasterization.Layer;
import com.wacom.ink.rasterization.StrokePaint;
import com.wacom.ink.rasterization.StrokeRenderer;

import java.util.ArrayList;
import java.util.List;


public class WILLWriter {
    private final StrokePaint strokePaint;
    private final BlendMode blendMode;
    private final float scaleFactor;

    private List<Stroke> strokeSegments = new ArrayList<>();
    private List<Stroke> newStrokeSegments = new ArrayList<>();

    public WILLWriter(int color, BlendMode blendMode, float scaleFactor) {
        // for erasing, the whiteboard controller has a separate layer which it blends with BLENDMODE_ERASE, which means
        // we must draw with a color in the case of an erasing writer
        this.strokePaint = WhiteboardUtils.createStrokePaint(blendMode == BlendMode.BLENDMODE_NORMAL ? color : Color.BLACK);
        this.blendMode = blendMode;
        this.scaleFactor = scaleFactor;
    }

    public void appendStrokeSegment(Stroke stroke) {
        strokeSegments.add(stroke);
        newStrokeSegments.add(stroke);
    }

    public boolean hasNewStrokeSegments() {
        return newStrokeSegments.size() > 0;
    }

    public void renderAllSegments(StrokeRenderer strokeRenderer, Layer targetLayer) {
        draw(strokeSegments, strokeRenderer, targetLayer);
    }

    public void renderNewSegments(StrokeRenderer strokeRenderer, Layer targetLayer) {
        draw(newStrokeSegments, strokeRenderer, targetLayer);
        newStrokeSegments.clear();
    }

    public void draw(List<Stroke> segments, StrokeRenderer strokeRenderer, Layer targetLayer) {
        strokeRenderer.reset();
        strokeRenderer.setStrokePaint(strokePaint);
        for (Stroke segment : segments) {
            strokeRenderer.drawPoints(segment.getScaledPointsFloatBuffer(scaleFactor), 0, segment.getSize(), false);
        }
        strokeRenderer.blendStroke(targetLayer, blendMode);
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }
}
