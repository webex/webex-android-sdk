package com.cisco.spark.android.whiteboard.view;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.MatrixBuilder;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardContentRenderedEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardLoadContentsEvent;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.cisco.spark.android.whiteboard.view.writer.LocalWILLWriter;
import com.cisco.spark.android.whiteboard.view.writer.SendToChatSnapshotRunnable;
import com.cisco.spark.android.whiteboard.view.writer.WILLWriter;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.wacom.ink.rasterization.BlendMode;
import com.wacom.ink.rasterization.DisposableException;
import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;
import com.wacom.ink.rasterization.StrokeRenderer;
import com.wacom.ink.rendering.EGLRenderingContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;
import rx.Observable;

public class WhiteboardController {

    private static final int SNAPSHOT_RUNNABLE_PENDING = 1;

    private final Gson gson;
    private final SdkClient sdkClient;
    private final WhiteboardService whiteboardService;
    private final EventBus bus;
    private final WhiteboardCache whiteboardCache;

    private SchedulerProvider schedulerProvider;
    private JsonParser jsonParser;

    private InkCanvas inkCanvas;
    private Layer strokesLayer;
    private Layer currentFrameLayer;
    private Layer viewLayer;
    private Layer pendingStrokesLayer;
    private Layer helperStrokeLayer;
    private Layer remoteWritersLayer;
    private SurfaceView surfaceView;
    private SnapshotHandler snapShotHandler;
    private StrokeRenderer helperStrokeRenderer;

    private float renderScaleFactor = 1.0f;
    private float offsetX = 0.f;
    private float offsetY = 0.f;
    private PointF focalPoint = new PointF(0, 0);

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public PointF getFocalPoint() {
        return focalPoint;
    }

    private boolean ready;

    public float getRenderScaleFactor() {
        return renderScaleFactor;
    }

    public void setRenderScaleFactor(float renderScaleFactor) {
        this.renderScaleFactor = renderScaleFactor;
    }

    public synchronized void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    private float scaleFactor = 1;

    private Map<Integer, LocalWILLWriter> localWriters;
    private Map<String, WILLWriter> remoteWriters;
    private Map<UUID, LocalWILLWriter> writersBeingSaved;

    public JsonArray allContents;

    public boolean isRedrawOfPendingLayerNeeded = false;
    private boolean hasFrameBeenRendered = false;
    private boolean isFrameRenderNeeded = false;
    private Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long l) {
            hasFrameBeenRendered = false;
            if (isFrameRenderNeeded) {
                renderView();
                isFrameRenderNeeded = false;
            }
        }
    };
    private boolean isFullRedrawOfRemoteWritersNeeded = false;

    public WhiteboardController(Gson gson, SdkClient sdkClient, WhiteboardService whiteboardService,
                                SurfaceView surfaceView, EventBus bus, SchedulerProvider schedulerProvider,
                                WhiteboardCache whiteboardCache) {
        this.gson = gson;
        this.sdkClient = sdkClient;
        this.whiteboardService = whiteboardService;
        this.bus = bus;
        this.schedulerProvider = schedulerProvider;
        this.whiteboardCache = whiteboardCache;

        jsonParser = new JsonParser();
        localWriters = new ConcurrentHashMap<>(WhiteboardConstants.MAX_TOUCH_POINTERS);
        remoteWriters = new ConcurrentHashMap<>();
        writersBeingSaved = new ConcurrentHashMap<>();
        this.surfaceView = surfaceView;
        allContents = new JsonArray();
    }

    public void initCanvas(SurfaceHolder surfaceHolder) {
        initCanvas(surfaceHolder, 0, 0);
    }

    public void initCanvas(SurfaceHolder surfaceHolder, int width, int height) {
        if (inkCanvas != null && !inkCanvas.isDisposed()) {
            releaseResources();
        }

        int realWidth = Math.max(width, surfaceView.getWidth());
        int realHeight = Math.max(height, surfaceView.getHeight());
        setScaleFactor((float) realWidth / 1600);

        inkCanvas = InkCanvas.create(surfaceHolder, new EGLRenderingContext.EGLConfiguration());
        viewLayer = inkCanvas.createViewLayer(realWidth, realHeight);
        strokesLayer = inkCanvas.createLayer(realWidth, realHeight);
        currentFrameLayer = inkCanvas.createLayer(realWidth, realHeight);
        pendingStrokesLayer = inkCanvas.createLayer(realWidth, realHeight);
        helperStrokeLayer = inkCanvas.createLayer(realWidth, realHeight);
        helperStrokeRenderer = new StrokeRenderer(inkCanvas, WhiteboardUtils.getDefaultStrokePaint(),
                3, helperStrokeLayer, null);
        remoteWritersLayer = inkCanvas.createLayer(realWidth, realHeight);

        clearWhiteboardLocal();
        snapShotHandler = new SnapshotHandler(Looper.getMainLooper());
        ready = true;
        Ln.i("Whiteboard has been initiated and is ready");
    }

    public void releaseResources() {
        Ln.i("Releasing whiteboard resources");
        ready = false;

        if (inkCanvas != null && !inkCanvas.isDisposed()) {
            inkCanvas.dispose();
        }
        if (helperStrokeRenderer != null && !helperStrokeRenderer.isDisposed()) {
            helperStrokeRenderer.dispose();
        }
        for (LocalWILLWriter localWILLWriter : localWriters.values()) {
            localWILLWriter.dispose();
        }

        remoteWriters.clear();
    }

    public void loadWhiteboard(Whiteboard whiteboard) {
        drawStrokes(whiteboard.getStrokes(), null);
        renderView();
    }

    public void drawStrokes(List<Stroke> strokesList, Matrix matrix) {
        try {
            helperStrokeRenderer.reset();
            for (Stroke stroke : strokesList) {
                helperStrokeRenderer.setStrokePaint(WhiteboardUtils.createStrokePaint(stroke.getColor()));
                helperStrokeRenderer.drawPoints(stroke.getScaledPointsFloatBuffer(getScaleFactor()), 0, stroke.getSize(), true);
                helperStrokeRenderer.blendStroke(strokesLayer, stroke.getBlendMode());
            }
            inkCanvas.setTarget(currentFrameLayer);
            inkCanvas.clearColor();
            inkCanvas.drawLayer(strokesLayer, matrix, BlendMode.BLENDMODE_NORMAL);
        } catch (DisposableException e) {
            Ln.e(e.getMessage());
        }
    }

    public void drawToCurrentFrameLayer() {
        helperStrokeRenderer.reset();
        for (Map.Entry<UUID, LocalWILLWriter> entry : writersBeingSaved.entrySet()) {
            LocalWILLWriter writer = entry.getValue();
            if (writer.isSaved()) {
                writer.renderStroke(helperStrokeRenderer);
                helperStrokeRenderer.blendStroke(strokesLayer, writer.getBlendMode());
                allContents.add(buildPersistedContentJson(writer));
                writersBeingSaved.remove(entry.getKey());
                isRedrawOfPendingLayerNeeded = true;
            }
        }

        if (isRedrawOfPendingLayerNeeded) {
            redrawStrokesBeingSaved();
        }

        inkCanvas.setTarget(currentFrameLayer);
        inkCanvas.clearColor();

        inkCanvas.drawLayer(strokesLayer, BlendMode.BLENDMODE_NORMAL);

        inkCanvas.drawLayer(pendingStrokesLayer, BlendMode.BLENDMODE_NORMAL);

        drawErasesBeingSaved();

        for (LocalWILLWriter writer : localWriters.values()) {
            writer.blendStrokeUpdateAreaToLayer(currentFrameLayer);
        }

        drawRemoteWriters();
    }

    private void drawRemoteWriters() {
        if (isFullRedrawOfRemoteWritersNeeded) {
            inkCanvas.clearLayer(remoteWritersLayer);
        }
        if (!remoteWriters.isEmpty()) {
            helperStrokeRenderer.reset();
            List<WILLWriter> remoteErasers = new ArrayList<>();
            for (Map.Entry<String, WILLWriter> entry : remoteWriters.entrySet()) {
                WILLWriter writer = entry.getValue();
                if (writer.getBlendMode() == BlendMode.BLENDMODE_NORMAL) {
                    if (isFullRedrawOfRemoteWritersNeeded) {
                        writer.renderAllSegments(helperStrokeRenderer, remoteWritersLayer);
                    } else {
                        if (writer.hasNewStrokeSegments()) {
                            writer.renderNewSegments(helperStrokeRenderer, remoteWritersLayer);
                        }
                    }
                } else if (writer.getBlendMode() == BlendMode.BLENDMODE_ERASE) {
                    remoteErasers.add(writer);
                }
            }
            inkCanvas.setTarget(currentFrameLayer);
            inkCanvas.drawLayer(remoteWritersLayer, BlendMode.BLENDMODE_NORMAL);

            for (WILLWriter remoteEraser : remoteErasers) {
                remoteEraser.renderAllSegments(helperStrokeRenderer, currentFrameLayer);
            }
        }
        isFullRedrawOfRemoteWritersNeeded = false;
    }

    private void redrawStrokesBeingSaved() {
        helperStrokeRenderer.reset();
        inkCanvas.clearLayer(pendingStrokesLayer);
        for (LocalWILLWriter writer : writersBeingSaved.values()) {
            if (writer.getBlendMode() == BlendMode.BLENDMODE_NORMAL) {
                writer.renderStroke(helperStrokeRenderer);
                helperStrokeRenderer.blendStroke(pendingStrokesLayer, BlendMode.BLENDMODE_NORMAL);
            }
        }
    }

    private void drawErasesBeingSaved() {
        for (LocalWILLWriter writer : writersBeingSaved.values()) {
            if (writer.getBlendMode() == BlendMode.BLENDMODE_ERASE) {
                writer.renderStroke(helperStrokeRenderer);
                helperStrokeRenderer.blendStroke(currentFrameLayer, BlendMode.BLENDMODE_ERASE);
            }
        }
    }

    public void renderView() {
        if (!hasFrameBeenRendered) {
            if (inkCanvas != null && !inkCanvas.isDisposed()) {
                hasFrameBeenRendered = true;
                drawToCurrentFrameLayer();

                inkCanvas.clearLayer(viewLayer, Color.WHITE);
                inkCanvas.setTarget(viewLayer);
                inkCanvas.drawLayer(currentFrameLayer, BlendMode.BLENDMODE_NORMAL);

                Choreographer.getInstance().postFrameCallback(frameCallback);
                inkCanvas.invalidate();
            }
        } else {
            isFrameRenderNeeded = true;
        }
    }

    public void renderViewWithMatrix(Matrix matrix) {
        if (inkCanvas != null && !inkCanvas.isDisposed()) {
            drawToCurrentFrameLayer();

            inkCanvas.clearLayer(viewLayer, Color.WHITE);
            inkCanvas.setTarget(viewLayer);
            inkCanvas.drawLayer(currentFrameLayer, matrix, BlendMode.BLENDMODE_NORMAL);

            inkCanvas.invalidate();
        }
    }

    public void renderContents(List<Content> contents) {
        processContentPayload(contents)
            .subscribe(strokes -> {
                drawStrokes(strokes, null);
                renderView();
            }, Ln::e);
    }

    public void renderContentsWithMatrix(List<Content> contents, Matrix matrix) {
        renderContentsWithMatrix(contents, matrix, null);
    }

    public void renderContentsWithMatrix(List<Content> contents, Matrix matrix, String cacheBoardId) {
        processContentPayload(contents)
            .subscribe(strokes -> {
                if (cacheBoardId != null) {
                    whiteboardCache.initAndStartRealtimeForBoard(cacheBoardId, strokes);
                }
                drawStrokes(strokes, matrix);
                renderViewWithMatrix(matrix);
                cacheContentsIntoImage();
            }, Ln::e);
    }

    public Observable<List<Stroke>> parseContentsIntoStrokes(List<Content> contents) {
        if (contents == null) return null;
        return Observable.from(contents)
                .subscribeOn(schedulerProvider.newThread())
                .map(this::parsePayload)
                .map(this::generateStrokeByContent)
                .toList();
    }

    private Observable<List<Stroke>> processContentPayload(List<Content> contents) {
        return Observable.from(contents)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .map(this::parsePayload)
                .map(this::generateStrokeByContent)
                .toList();
    }

    private boolean isCurve(JsonObject parsedContent) {
        return parsedContent != null
                && WhiteboardConstants.CURVE_TYPE.equalsIgnoreCase(
                    parsedContent.get(WhiteboardConstants.CONTENT_TYPE).getAsString());
    }

    private JsonObject parsePayload(Content content) {
        if (content == null) return null;
        JsonObject result = null;
        try {
            JsonElement parsedContent = jsonParser.parse(content.getPayload());
            if (parsedContent != null && parsedContent.isJsonObject()) {
                result = parsedContent.getAsJsonObject();
            }
        } catch (JsonSyntaxException e) {
            Ln.e(e, "JSON parse failed");
        }
        return result;
    }
    @NonNull
    private Stroke generateStrokeByContent(JsonObject payload) {
        if (payload == null) return null;
        return isCurve(payload) ? WhiteboardUtils.createStroke(payload, gson) : null;
    }

    public void drawStrokesWithMatrix(List<Stroke> strokes, Matrix matrix) {
        if (strokes != null && !strokes.isEmpty()) {
            drawStrokes(strokes, matrix);
            renderViewWithMatrix(matrix);
        }
    }

    public void startNewEvent() {
        for (LocalWILLWriter writer : localWriters.values()) {
            writer.startNewEvent();
        }
    }

    public void persistFinishedStrokes() {
        for (Map.Entry<UUID, LocalWILLWriter> entry : writersBeingSaved.entrySet()) {
            LocalWILLWriter writer = entry.getValue();
            if (writer != null && !writer.isSaving() && !writer.isSaved()) {
                writer.setSaving(true);

                List<Content> contentRequests = new ArrayList<>();
                splitUpCurve(writer, (writer1, start, end) ->
                        contentRequests.add(buildStrokeContent(buildPersistedContentJson(writer1, start, end))));

                postStroke(contentRequests, entry.getKey());
            }
        }
    }

    public void splitUpCurve(LocalWILLWriter writer, SplitUpCurveCallback callback) {
        int index = 0;
        int pointsLimit = 1000 * writer.getStride(); // 1000 points
        int pointOffset = 3 * writer.getStride();  // also get the previous 3 last points to avoid gaps

        while (index * pointsLimit < writer.getPoints().length) {

            int start = index * pointsLimit;
            int end = Math.min(start + pointsLimit, writer.getPoints().length);

            if (start - pointOffset >= 0)
                start = start - pointOffset;
            callback.execute(writer, start, end);
            index++;
        }
    }

    public Content buildStrokeContent(JsonObject contentJson) {
        Channel currentChannel = whiteboardService.getCurrentChannel();
        String keyUrl = null;
        if (currentChannel != null) {
            keyUrl = currentChannel.getDefaultEncryptionKeyUrl() != null ? currentChannel.getDefaultEncryptionKeyUrl().toString() : null;
        }
        return new Content(Content.CONTENT_TYPE, sdkClient.getDeviceType(), keyUrl, contentJson.toString());
    }

    public void clearWhiteboardLocal() {
        resetWhiteboardLocal();
        renderView();
    }

    public void resetWhiteboardLocal() {
        allContents = new JsonArray();
        if (writersBeingSaved.size() != 0) {
            for (Map.Entry<UUID, LocalWILLWriter> entry : writersBeingSaved.entrySet()) {
                entry.getValue().dispose();
            }
            writersBeingSaved.clear();
        }
        if (localWriters.size() != 0) {
            for (Map.Entry<Integer, LocalWILLWriter> entry : localWriters.entrySet()) {
                entry.getValue().dispose();
            }
            localWriters.clear();
        }
        remoteWriters.clear();
        isRedrawOfPendingLayerNeeded = true;
        isFullRedrawOfRemoteWritersNeeded = true;
        if (inkCanvas != null) {

            // The try catch block here is used to swallow the exception from WACOM sdk.
            // to prevent the Android client from crashing
            try {
                inkCanvas.clearLayer(pendingStrokesLayer);
                inkCanvas.clearLayer(strokesLayer);
                inkCanvas.clearLayer(helperStrokeLayer);
                inkCanvas.clearLayer(viewLayer);
                inkCanvas.clearLayer(remoteWritersLayer);
                inkCanvas.clearLayer(currentFrameLayer);
            } catch (NullPointerException e) {
                Ln.w(e);
            }
        }
    }

    public void clearWhiteboardRemote(JsonObject clearBoardJson) {
        if (!whiteboardService.isLocal()) { //is the whiteboard is Private
            whiteboardService.realtimeMessage(clearBoardJson);
        }

        whiteboardService.clearBoard();
        takeSnapshotDelayed();
    }

    private JsonObject buildPersistedContentJson(LocalWILLWriter writer) {
        return buildPersistedContentJson(writer, 0, writer.getPoints().length);
    }

    public JsonObject buildPersistedContentJson(LocalWILLWriter writer, int indexStart, int indexEnd) {
        JsonObject json = new JsonObject();
        json.addProperty(WhiteboardConstants.CURVE_ID, writer.getWriterId().toString());
        json.addProperty(WhiteboardConstants.CONTENT_TYPE, WhiteboardConstants.CURVE_TYPE);
        json.addProperty(WhiteboardConstants.NAME, WhiteboardConstants.CONTENT_COMMIT);
        json.addProperty(WhiteboardConstants.ACTION, WhiteboardConstants.CONTENT_COMMIT);
        json.addProperty(WhiteboardConstants.STRIDE, writer.getStride());
        json.addProperty(WhiteboardConstants.DRAW_MODE, writer.getBlendMode() ==
                BlendMode.BLENDMODE_ERASE ? WhiteboardConstants.ERASE : WhiteboardConstants.NORMAL);
        json.add(WhiteboardConstants.COLOR, WhiteboardUtils.convertColorIntToJson(writer.getColor()));
        json.add(WhiteboardConstants.CURVE_POINTS, buildCurvePoints(writer, indexStart, indexEnd));
        return json;
    }

    private JsonArray buildCurvePoints(LocalWILLWriter writer, int indexStart, int indexEnd) {
        float[] points = Arrays.copyOfRange(writer.getPoints(), indexStart, indexEnd);
        float[] scaledPoints = WhiteboardUtils.scaleRawOutputPoints(points, getScaleFactor());
        return gson.toJsonTree(scaledPoints).getAsJsonArray();
    }

    private void postStroke(List<Content> contentRequests, UUID id) {
        whiteboardService.saveContents(contentRequests, id.toString());
        takeSnapshotDelayed();
    }

    public synchronized float getScaleFactor() {
        return scaleFactor;
    }

    @SuppressWarnings("UnusedDeclaration")
    public InkCanvas getInkCanvas() {
        return inkCanvas;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Layer getStrokesLayer() {
        return strokesLayer;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Layer getCurrentFrameLayer() {
        return currentFrameLayer;
    }

    @SuppressWarnings("UnusedDeclaration")
    public LocalWILLWriter getLocalWriter(int pointerId) {
        if (pointerId < 0 || pointerId >= WhiteboardConstants.MAX_TOUCH_POINTERS) {
            throw new IllegalArgumentException("pointerId must be between 0 and WhiteboardConstants.MAX_TOUCH_POINTERS");
        }

        return localWriters.get(pointerId);
    }

    public Map<Integer, LocalWILLWriter> getLocalWriters() {
        return localWriters;
    }

    public Map<UUID, LocalWILLWriter> getWritersBeingSaved() {
        return writersBeingSaved;
    }

    public LocalWILLWriter createNewLocalWILLWriter(int drawColor, BlendMode strokeBlendMode, float scaleFactor) {
        // We need to reset the target layer for the ink canvas to prevent WILL weirdness:
        // when a writer is rendered, it is set as the current layer in the ink canvas.
        // If the writer, and thus its layer, has been disposed, the current layer of the
        // ink canvas points to a disposed canvas. When you create a new Layer in WILL, it
        // will temporary set the current layer to the new one, then set it back to the old
        // one. When setting back to the old one, it checks if it has been disposed, and if
        // it has, WILL crashes complaining that we are trying to set a disposed layer as
        // the current layer. This is a race condition that can occur when tapping quickly on
        // the screen, thus we need to set the target to something valid before trying to use
        // WILL in case it creates a new writer (and a new surface)
        try {
            inkCanvas.setTarget(currentFrameLayer);
            return new LocalWILLWriter(drawColor, strokeBlendMode, inkCanvas, surfaceView.getWidth(),
                    surfaceView.getHeight(), scaleFactor);
        } catch (Exception e) {
            Ln.w(e, "Create LocalWILLWriter failed");
            return null;
        }
    }

    public WILLWriter createNewWILLWriter(String writerId, int strokeColor, BlendMode blendMode) {
        // see comment above
        inkCanvas.setTarget(currentFrameLayer);
        WILLWriter writer = new WILLWriter(strokeColor, blendMode, getScaleFactor());
        remoteWriters.put(writerId, writer);
        return writer;
    }

    public void moveWriterToBeingSaved(LocalWILLWriter writer, int pointerId) {
        localWriters.remove(pointerId);
        writersBeingSaved.put(writer.getWriterId(), writer);
        isRedrawOfPendingLayerNeeded = true;
    }

    public void removeWriter(UUID writerId) {
        LocalWILLWriter writer = writersBeingSaved.get(writerId);
        if (writer != null) {
            isRedrawOfPendingLayerNeeded = true;
            writersBeingSaved.remove(writerId);
            writer.dispose();
        }
    }

    public void removeRemoteWriter(String remoteWriterKey) {
        WILLWriter writer = remoteWriters.get(remoteWriterKey);
        if (writer != null) {
            remoteWriters.remove(remoteWriterKey);
            isFullRedrawOfRemoteWritersNeeded = true;
        }
    }

    public WILLWriter getRemoteWriter(String remoteWriterKey) {
        return remoteWriters.get(remoteWriterKey);
    }

    public void commitRemoteWriter(String remoteWriterKey, List<Content> contentsList) {

        Matrix matrix = new MatrixBuilder()
                .setScaleFactor(renderScaleFactor)
                .setTranslateX(offsetX)
                .setTranslateY(offsetY)
                .build();

        removeRemoteWriter(remoteWriterKey);
        renderContentsWithMatrix(contentsList, matrix);
    }

    public void cancelRemoteWriter(String remoteWriterKey) {

        Matrix matrix = new MatrixBuilder()
                .setScaleFactor(renderScaleFactor)
                .setFocalPoint(focalPoint)
                .build();

        removeRemoteWriter(remoteWriterKey);
        renderViewWithMatrix(matrix);
    }

    public void loadContents(WhiteboardLoadContentsEvent event) {
        if (event.shouldResetBoard()) {
            resetWhiteboardLocal();
        }

        Matrix matrix = new MatrixBuilder()
                .setScaleFactor(renderScaleFactor)
                .setFocalPoint(focalPoint)
                .build();

        renderContentsWithMatrix(event.getContents(), matrix, event.getBoardId());
    }

    public void cacheContentsIntoImage() {
        Ln.i("Cached Content snapshot refreshed");
        Bitmap bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
        inkCanvas.readPixels(currentFrameLayer, bitmap, 0, 0, 0, 0, surfaceView.getWidth(), surfaceView.getHeight());
        bus.post(new WhiteboardContentRenderedEvent(bitmap));
    }

    private void takeSnapshotDelayed() {
        abortSnapshot();
        SnapshotRunnable snapshotRunnable = new SnapshotRunnable(whiteboardService, surfaceView, inkCanvas, viewLayer,
                whiteboardService.getCurrentChannel(), null, bus);
        snapShotHandler.sendMessageDelayed(Message.obtain(snapShotHandler, SNAPSHOT_RUNNABLE_PENDING, snapshotRunnable),
                                           WhiteboardConstants.SNAPSHOT_UPLOAD_DELAY_MILLIS);
    }

    public void takeSnapshotImmediate(final SnapshotRunnable.OnSnapshotUploadListener onSnapshotUploadListener) {
        // Only take snapshot at once if a delayed one was in the queue
        if (snapShotHandler != null && snapShotHandler.hasMessages(SNAPSHOT_RUNNABLE_PENDING)) {
            abortSnapshot();
            SnapshotRunnable snapshotRunnable = new SnapshotRunnable(whiteboardService, surfaceView, inkCanvas,
                    viewLayer, whiteboardService.getCurrentChannel(), onSnapshotUploadListener, bus);
            snapShotHandler.sendMessage(Message.obtain(snapShotHandler, SNAPSHOT_RUNNABLE_PENDING, snapshotRunnable));
        } else if (onSnapshotUploadListener != null) {
            onSnapshotUploadListener.onSyncUIWorkComplete();
        }
    }

    public void takeSnapshotImmediateForChat() {
        //To use currentFrameLayer to take snapshot, due to viewLayer rendered only if user draw something(in touch event) on whiteboard
        //Need to check if currentFrameLayer with background image when Background image available in native whiteboard
        if (snapShotHandler != null) {
            SendToChatSnapshotRunnable sendToChatSnapshotRunnable = new SendToChatSnapshotRunnable(whiteboardService, surfaceView, inkCanvas,
                    currentFrameLayer, whiteboardService.getCurrentChannel(), null, bus);
            snapShotHandler.sendMessage(Message.obtain(snapShotHandler, SNAPSHOT_RUNNABLE_PENDING, sendToChatSnapshotRunnable));
        }
    }

    public void abortSnapshot() {
        if (snapShotHandler == null) return;
        snapShotHandler.removeMessages(SNAPSHOT_RUNNABLE_PENDING);
    }

    public boolean isReady() {
        return ready;
    }

    private static class SnapshotHandler extends Handler {

        public SnapshotHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SNAPSHOT_RUNNABLE_PENDING) {
                BaseSnapshotRunnable runnable = (BaseSnapshotRunnable) msg.obj;
                runnable.run();
            }
        }
    }

    public interface SplitUpCurveCallback {
        void execute(LocalWILLWriter writer, int start, int end);
    }

    public void renderImage(Bitmap bitmap, Matrix matrix, int width, int height) {
        inkCanvas.clearLayer(currentFrameLayer, Color.WHITE);
        inkCanvas.setTarget(currentFrameLayer);
        inkCanvas.writePixels(strokesLayer, bitmap, 0, 0, 0, 0, width, height);
        renderViewWithMatrix(matrix);
    }

}
