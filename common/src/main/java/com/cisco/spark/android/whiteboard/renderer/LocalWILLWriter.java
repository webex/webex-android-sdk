package com.cisco.spark.android.whiteboard.renderer;

import android.graphics.Color;
import android.view.MotionEvent;

import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.wacom.ink.path.PathBuilder;
import com.wacom.ink.path.PathUtils;
import com.wacom.ink.path.SpeedPathBuilder;
import com.wacom.ink.rasterization.BlendMode;
import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;
import com.wacom.ink.rasterization.StrokePaint;
import com.wacom.ink.rasterization.StrokeRenderer;
import com.wacom.ink.smooth.MultiChannelSmoothener;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocalWILLWriter {

    private final UUID writerId;
    private final BlendMode blendMode;
    private final InkCanvas inkCanvas;
    private final SpeedPathBuilder pathBuilder;
    private final LayerPool.ReusableLayer strokeLayer;
    private final LayerPool.ReusableLayer strokeWithPreliminaryLayer;
    private final StrokeRenderer strokeRenderer;
    private final MultiChannelSmoothener smoothener;
    private FloatBuffer pathCacheBuffer;
    private List<float[]> changedPointsCurrentEvent;
    private boolean saving;
    private boolean saved;
    private float[] points;
    private StrokePaint strokePaint;

    public LocalWILLWriter(int color, BlendMode blendMode, InkCanvas inkCanvas, LayerPool.ReusableLayer strokeLayer, LayerPool.ReusableLayer strokeWithPreliminaryLayer, float scaleFactor) {
        this.writerId = UUID.randomUUID();
        this.blendMode = blendMode;
        this.inkCanvas = inkCanvas;
        this.strokePaint = WhiteboardUtils
                                   .createStrokePaint(blendMode == BlendMode.BLENDMODE_ERASE ? Color.BLACK : color);
        this.strokeLayer = strokeLayer;
        this.strokeWithPreliminaryLayer = strokeWithPreliminaryLayer;

        pathBuilder = new SpeedPathBuilder();
        pathBuilder.setNormalizationConfig(WhiteboardConstants.PATH_BUILDER_NORMALIZATION_CONFIG_MIN_VALUE,
                                           WhiteboardConstants.PATH_BUILDER_NORMALIZATION_CONFIG_MAX_VALUE);
        pathBuilder.setMovementThreshold(WhiteboardConstants.PATH_BUILDER_MOVEMENT_THRESHOLD);
        boolean inEraseMode = blendMode.equals(BlendMode.BLENDMODE_ERASE);
        configurePathSize(inEraseMode, scaleFactor);

        strokeRenderer = new StrokeRenderer(inkCanvas, strokePaint, pathBuilder.getStride(), strokeLayer.getLayer(),
                                            strokeWithPreliminaryLayer.getLayer());
        smoothener = new MultiChannelSmoothener(pathBuilder.getStride());
        smoothener.enableChannel(2);
        // the internal cache in Will holds two points
        pathCacheBuffer = FloatBuffer.allocate(pathBuilder.getStride() * 2);
        changedPointsCurrentEvent = new ArrayList<>();
    }

    public boolean buildPath(int pointerIndex, MotionEvent event) {
        int action = event.getActionMasked();
        boolean isActionMine = pointerIndex == event.getActionIndex();
        if (isActionMine && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)) {
            // Reset the smoothener instance when starting to generate a new path.
            smoothener.reset();
        }

        PathUtils.Phase phase = isActionMine ? WhiteboardUtils.getPhaseFromMotionEvent(event) : PathUtils.Phase.MOVE;
        // Add the current input point to the path builder
        boolean pathBuilderHasFinished = addInputToPathBuilder(phase, event.getX(pointerIndex),
                                                               event.getY(pointerIndex), event.getEventTime(), true);

        boolean drawingFinished =
                isActionMine && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP);
        boolean strokeEnded = drawingFinished && pathBuilderHasFinished;

        if (strokeEnded) {
            prepareForPersistence();
        }
        return strokeEnded;
    }

    public boolean canBePersisted() {
        return pathBuilder.getPathBuffer() != null;
    }

    public void prepareForPersistence() {
        strokePaint = strokeRenderer.getStrokePaint();
        points = new float[pathBuilder.getPathSize()];
        int previousPosition = pathBuilder.getPathBuffer().position();
        pathBuilder.getPathBuffer().get(points);
        pathBuilder.getPathBuffer().position(previousPosition);
    }

    public void drawHistoricalStrokes(int pointerIndex, MotionEvent event) {
        pathCacheBuffer.clear();
        for (int i = 0; i < event.getHistorySize(); i++) {
            boolean needsToDrawStroke = addInputToPathBuilder(PathUtils.Phase.MOVE,
                                                              event.getHistoricalX(pointerIndex, i),
                                                              event.getHistoricalY(pointerIndex, i),
                                                              event.getHistoricalEventTime(i), false);
            if (needsToDrawStroke) {
                drawPoints(true, false);
            }
        }
    }

    public void drawPoints(boolean isMoveEvent, boolean finishedRendering) {
        strokeRenderer.drawPoints(pathBuilder.getPathBuffer(), pathBuilder.getPathLastUpdatePosition(),
                                  pathBuilder.getAddedPointsSize(), finishedRendering);
        strokeRenderer.drawPrelimPoints(pathBuilder.getPreliminaryPathBuffer(), 0,
                                        pathBuilder.getFinishedPreliminaryPathSize());

        if (isMoveEvent) {
            int pathSize = pathBuilder.getAddedPointsSize();
            if (pathSize > 0) {
                float[] points = new float[pathSize];
                for (int i = 0; i < pathSize; i++) {
                    points[i] = pathBuilder.getPathBuffer().get(pathBuilder.getPathLastUpdatePosition() + i);
                }
                changedPointsCurrentEvent.add(points);
            }
        }
    }

    public void blendStrokeUpdateAreaToLayer(Layer destinationLayer) {
        // NOTE: we are not using the StrokeRenderer.blendStrokeUpdatedArea() as it will only blend
        // the updated area and we render the whole frame. A side effect is that the
        // getStrokeUpdatedArea() will then contain the bounding box for the whole stroke, including
        // the preliminary path, whereas getTotalArea() is only the area of the stroke that has been
        // processed. We also only need to draw the strokeWithPreliminaryLayer as it has both the
        // processed stroke and the preliminary path. For more fun read, checkout the Will source
        inkCanvas.setTarget(destinationLayer, strokeRenderer.getStrokeUpdatedArea());
        inkCanvas.drawLayer(this.strokeWithPreliminaryLayer.getLayer(), blendMode);
    }

    public void startNewEvent() {
        changedPointsCurrentEvent.clear();
    }

    private boolean addInputToPathBuilder(PathUtils.Phase phase, float x, float y, long eventTime,
                                          boolean forceSmooth) {
        FloatBuffer part = pathBuilder.addPoint(phase, x, y, eventTime);
        int partSize = pathBuilder.getPathPartSize();

        if (partSize > 0) {
            part.rewind();
            for (int i = 0; i < partSize; i++) {
                pathCacheBuffer.put(part.get(i));
            }

            // Draw when there is 2 points in the buffer (x, y, width * 2)
            if (pathCacheBuffer.position() == 6 || forceSmooth) {
                MultiChannelSmoothener.SmoothingResult smoothingResult;

                pathCacheBuffer.flip();
                part.clear();
                while (pathCacheBuffer.hasRemaining()) {
                    part.put(pathCacheBuffer.get());
                }

                smoothingResult = smoothener.smooth(part, part.position(), (phase == PathUtils.Phase.END));
                // Add the returned control points (aka partial path) to the path builder.
                pathBuilder.addPathPart(smoothingResult.getSmoothedPoints(), smoothingResult.getSize());
                // Create a preliminary path.
                FloatBuffer preliminaryPath = pathBuilder.createPreliminaryPath();
                if (pathBuilder.getPreliminaryPathSize() > 0) {
                    // Smoothen the preliminary path's control points (return inform of a partial path).
                    smoothingResult = smoothener.smooth(preliminaryPath, pathBuilder.getPreliminaryPathSize(), true);
                    // Add the smoothed preliminary path to the path builder.
                    pathBuilder.finishPreliminaryPath(smoothingResult.getSmoothedPoints(), smoothingResult.getSize());
                }
                pathCacheBuffer.clear();
                return true;
            }
        }

        return false;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public boolean isSaving() {
        return saving;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }


    public UUID getWriterId() {
        return writerId;
    }

    public int getColor() {
        return strokeRenderer.getStrokePaint().getColor();
    }

    public List<float[]> getChangedPointsCurrentEvent() {
        return changedPointsCurrentEvent;
    }

    public void dispose() {
        if (strokeRenderer != null && !strokeRenderer.isDisposed()) {
            strokeRenderer.dispose();
        }
        if (strokeLayer != null) {
            strokeLayer.dispose();
        }
        if (strokeWithPreliminaryLayer != null) {
            strokeWithPreliminaryLayer.dispose();
        }
    }

    private void configurePathSize(boolean isInEraseMode, float scaleFactor) {
        float pathMinWidth, pathMaxWidth, pathInitialWidth, pathEndWidth;
        boolean pathThinWhenFaster;
        if (isInEraseMode) {
            pathMinWidth = pathMaxWidth = pathInitialWidth = pathEndWidth = WhiteboardConstants.ERASER_WIDTH * scaleFactor;
            pathThinWhenFaster = false;
        } else {
            pathMinWidth = WhiteboardConstants.PEN_MIN_WIDTH * scaleFactor;
            pathMaxWidth = WhiteboardConstants.PEN_MAX_WIDTH * scaleFactor;
            pathInitialWidth = WhiteboardConstants.PEN_INITIAL_WIDTH * scaleFactor;
            pathEndWidth = WhiteboardConstants.PEN_MAX_WIDTH * scaleFactor;
            pathThinWhenFaster = true;
        }

        pathBuilder.setPropertyConfig(PathBuilder.PropertyName.Width, pathMinWidth, pathMaxWidth, pathInitialWidth,
                                      pathEndWidth, WhiteboardConstants.PATH_FUNCTION,
                                      WhiteboardConstants.PATH_FUNCTION_PARAMETER, pathThinWhenFaster);
    }

    public float[] getPoints() {
        return points;
    }

    public int getStride() {
        return pathBuilder.getStride();
    }

    public void renderStroke(StrokeRenderer strokeRenderer) {
        FloatBuffer floatBuffer = pathBuilder.getPathBuffer();
        int bufferPosition = floatBuffer.position();
        int bufferSize = pathBuilder.getPathSize();
        strokeRenderer.setStrokePaint(strokePaint);
        strokeRenderer.drawPoints(floatBuffer, bufferPosition, bufferSize, true);
    }
}
