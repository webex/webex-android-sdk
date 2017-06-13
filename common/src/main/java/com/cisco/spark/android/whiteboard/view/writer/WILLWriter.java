package com.cisco.spark.android.whiteboard.view.writer;

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
        this.strokePaint = WhiteboardUtils.createStrokePaint(color);
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
        for (Stroke segment : segments) {
            strokeRenderer.setStrokePaint(strokePaint);
            strokeRenderer.drawPoints(segment.getScaledPointsFloatBuffer(scaleFactor), 0, segment.getSize(), false);
            strokeRenderer.blendStroke(targetLayer, blendMode);
        }
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }
}
