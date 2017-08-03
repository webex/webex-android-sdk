package com.cisco.spark.android.whiteboard.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardCreator;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.MatrixBuilder;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.SendToChatSnapshotRunnable;
import com.cisco.spark.android.whiteboard.view.SnapshotRunnable;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardContentRenderedEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardLoadContentsEvent;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.cisco.wx2.android.util.MotionEventBoundaryMapper;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.wacom.ink.rasterization.BlendMode;
import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;
import com.wacom.ink.rasterization.StrokeRenderer;
import com.wacom.ink.rendering.EGLRenderingContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.greenrobot.event.EventBus;
import rx.Observable;

import static android.graphics.Matrix.MTRANS_X;
import static android.graphics.Matrix.MTRANS_Y;
import static com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer.PersistenceEvent.PersistenceEventType.FINISH;
import static com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer.PersistenceEvent.PersistenceEventType.POINTERS;
import static com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer.PersistenceEvent.PersistenceEventType.STOP_THREAD;
import static com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer.PersistenceEvent.createFinish;
import static com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer.PersistenceEvent.createPointers;
import static com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer.PersistenceEvent.createStopThread;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_TYPE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CURVE_TYPE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SNAPSHOT_UPLOAD_DELAY_MILLIS;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WHITEBOARD_CONTENT_POINTS_LIMIT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.maxTouchPointers;

public class WhiteboardRenderer implements Choreographer.FrameCallback, OnScaleGestureListener {

    final WhiteboardLocalWriter whiteboardLocalWriter;
    private final WhiteboardRealtimeWriter whiteboardRealtimeWriter;

    private final WhiteboardService whiteboardService;
    private final WhiteboardCache whiteboardCache;
    private final EventBus bus;
    private final Gson gson;
    private final SdkClient sdkClient;
    private final Context context;
    private final Object lock = new Object();
    private WhiteboardSurface activeSurface;
    private Whiteboard activeWhiteboard;
    private WILLFrontBuffer frontBuffer;
    private final LinkedList<PendingSurface> pendingSurfaceHolders = new LinkedList<>();

    private int width;
    private int height;

    private boolean readOnlyMode;
    private volatile boolean frameCallbackPosted;

    private OnSizeChangedCallback onSizeChangedCallback;
    private List<RenderListener> renderListeners;

    private Queue<MotionEvent> motionEvents;

    private static final int SNAPSHOT_RUNNABLE_PENDING = 1;

    private static final float MIN_SCALE_FACTOR = 1.f;
    private static final float MAX_SCALE_FACTOR = 5.f;

    private static final int BACKGROUND_COLOR = Color.parseColor("#d8d8d8");

    private SchedulerProvider schedulerProvider;
    private JsonParser jsonParser;

    private InkCanvas inkCanvas;
    private Layer strokesLayer;
    private Layer currentFrameLayer;
    private Layer viewLayer;
    private Layer pendingStrokesLayer;
    private Layer helperStrokeLayer;
    private Layer remoteWritersLayer;
    private Layer remoteErasersLayer;
    private Layer backgroundLayer;
    private final SnapshotHandler snapShotHandler;
    private StrokeRenderer helperStrokeRenderer;

    private PointF zoomPanFocalPoint = new PointF();
    private Matrix zoomPanMatrix;
    private float[] zoomPanMatrixValues = new float[9];

    private float scaleFactor;
    private float offsetX;
    private float offsetY;

    private int layerWidth;
    private int layerHeight;
    private int surfaceWidth;
    private int surfaceHeight;

    // the renderMatrix is used to render the current frame layer on to the view layer, since the
    // frame layer is always a 16:9 ratio layer whereas the view layer occupies all the space it
    // can get. Since the points of the coordinates are already scaled using the scaleFactor, the
    // matrix is a translation to center the frame layer in the view layer before it applies the
    // zomPanMatrix matrix.
    private Matrix renderMatrix = new Matrix();

    // the inputMatrix is the inverse of the renderMatrix to map the coordinates of the motion events
    // to the frame layer. Thus the motion events needs to be transformed with this matrix to ensure
    // they map into the coordinate system of the frame layer
    private Matrix inputMatrix = new Matrix();

    private ScaleGestureDetector scaleDetector;

    private LayerPool layerPool;
    private UUID channelCreationRequestId;

    private MotionEventBoundaryMapper mapper;

    private volatile boolean ready;
    private Map<Integer, LocalWILLWriter> localWriters;
    private Map<String, WILLWriter> remoteWriters;
    private Map<UUID, LocalWILLWriter> writersBeingSaved;

    public JsonArray allContents;

    public boolean isRedrawOfPendingLayerNeeded = false;
    private boolean isFullRedrawOfRemoteWritersNeeded = false;
    private PersistenceThread persistenceThread;

    public WhiteboardRenderer(Gson gson, SdkClient sdkClient, WhiteboardService whiteboardService,
                              ApiTokenProvider apiTokenProvider, EventBus bus,
                              SchedulerProvider schedulerProvider, WhiteboardCache whiteboardCache,
                              Clock clock, Context context) {
        this.whiteboardService = whiteboardService;
        this.whiteboardCache = whiteboardCache;
        this.bus = bus;
        this.gson = gson;
        this.sdkClient = sdkClient;
        this.schedulerProvider = schedulerProvider;
        this.context = context;
        this.frontBuffer = new WILLFrontBuffer();
        bus.register(this);
        renderListeners = new ArrayList<>();
        motionEvents = new ConcurrentLinkedQueue<>();
        whiteboardLocalWriter = new WhiteboardLocalWriter(this);
        whiteboardRealtimeWriter = new WhiteboardRealtimeWriter(gson, bus, apiTokenProvider, whiteboardService, whiteboardCache, clock, schedulerProvider, this);
        jsonParser = new JsonParser();
        localWriters = new ConcurrentHashMap<>(maxTouchPointers);
        remoteWriters = new ConcurrentHashMap<>();
        writersBeingSaved = new ConcurrentHashMap<>();
        allContents = new JsonArray();
        layerPool = new LayerPool();

        snapShotHandler = new SnapshotHandler(Looper.getMainLooper());
        Choreographer.getInstance().postFrameCallback(this);
    }

    static class PersistenceEvent {
        enum PersistenceEventType {
            POINTERS,
            FINISH,
            STOP_THREAD
        }

        public static PersistenceEvent createPointers(List<WhiteboardLocalWriter.Pointer> pointers) {
            return new PersistenceEvent(POINTERS, pointers);
        }

        public static PersistenceEvent createFinish() {
            return new PersistenceEvent(FINISH, null);
        }

        public static PersistenceEvent createStopThread() {
            return new PersistenceEvent(STOP_THREAD, null);
        }

        private PersistenceEvent(PersistenceEventType type, List<WhiteboardLocalWriter.Pointer> pointers) {
            this.type = type;
            this.pointers = pointers;
        }

        public final PersistenceEventType type;
        public final List<WhiteboardLocalWriter.Pointer> pointers;
    }

    private void setZoomPanFocalPoint(PointF point) {
        zoomPanFocalPoint = point;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float level = detector.getScaleFactor();
        float scaleFactor = getZoomPanScale();
        if (scaleFactor == MAX_SCALE_FACTOR && level > 1 || scaleFactor == MIN_SCALE_FACTOR && level < 1) {
            return false;
        }
        scaleFactor *= level;

        if (scaleFactor < 1) {
            setOffset(0, 0);
        }
        scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(scaleFactor, MAX_SCALE_FACTOR));

        float deltaX = detector.getFocusX() - zoomPanFocalPoint.x;
        float deltaY = detector.getFocusY() - zoomPanFocalPoint.y;

        float transX = (1 - level) * zoomPanFocalPoint.x + level * zoomPanMatrixValues[MTRANS_X] + deltaX;
        float transY = (1 - level) * zoomPanFocalPoint.y + level * zoomPanMatrixValues[MTRANS_Y] + deltaY;

        updateMatrix(scaleFactor, transX, transY);
        setZoomPanFocalPoint(new PointF(detector.getFocusX(), detector.getFocusY()));
        renderView();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        setZoomPanFocalPoint(new PointF(detector.getFocusX(), detector.getFocusY()));
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    private class PersistenceThread extends Thread {

        private final static int PERSISTENCE_MESSAGE = 1;

        private class PersistenceHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == PERSISTENCE_MESSAGE) {
                    PersistenceEvent event = (PersistenceEvent) msg.obj;

                    switch(event.type) {
                        case POINTERS:
                            createBoardIfRequired();
                            sendRealtimeMessage(event.pointers);
                            persistFinishedStrokes();
                            break;
                        case FINISH:
                            createBoardIfRequired();
                            whiteboardLocalWriter.finishLocalInputs();
                            break;
                        case STOP_THREAD:
                            Looper.myLooper().quit();
                    }
                }
            }
        }

        private PersistenceHandler persistenceHandler;

        public PersistenceThread(String name) {
            super(name);
        }

        public void addEvent(PersistenceEvent event) {
            persistenceHandler.sendMessage(Message.obtain(persistenceHandler, PERSISTENCE_MESSAGE, event));
        }

        @Override
        public void run() {
            Looper.prepare();
            persistenceHandler = new PersistenceHandler();
            Looper.loop();
        }
    }

    private void createBoardIfRequired() {
        if (getWhiteboardService().getCurrentChannel() == null) {
            final Uri appKeyUrl = getWhiteboardService().getDefaultConversationKeyUrl();
            final Uri aclUrl = getWhiteboardService().getAclUrlLink();
            WhiteboardService.WhiteboardContext context = new WhiteboardService.WhiteboardContext(aclUrl, appKeyUrl, null, null);
            UUID channelCreationRequestId = getWhiteboardService().createBoard(new WhiteboardCreator.CreateData(context), null);
            if (channelCreationRequestId != null) {
                setChannelCreationRequestId(channelCreationRequestId);
            }
        }
    }

    void restoreCanvasIfNeeded() {
        if (activeSurface != null) {
            Whiteboard cachedWhiteboard = whiteboardCache.getWhiteboard(whiteboardService.getBoardId());
            restoreInkCanvas(activeSurface, cachedWhiteboard, width, height);
        }
    }

    public void enableScaleGestureDetector(Context context) {
        if (scaleDetector == null) {
            scaleDetector = new ScaleGestureDetector(context, this);
        }
    }

    public void surfaceCreated(WhiteboardSurface surface) {
        synchronized (lock) {
            Ln.i("Whiteboard surface created (%s)", surface);
            if (activeSurface == null) {
                Ln.i("Whiteboard surface created (%s) registering as active surface", surface);
                whiteboardRealtimeWriter.register();
                activeSurface = surface;
            } else {
                Ln.i("Whiteboard surface created (%s) but existing surface is registered, adding to pending queue", surface);
                pendingSurfaceHolders.add(new PendingSurface(surface));
            }
        }
    }

    public boolean readPixels(Bitmap bitmap, int x, int y, int width, int height) {
        if (inkCanvas != null && !inkCanvas.isDisposed()) {
            inkCanvas.readPixels(currentFrameLayer, bitmap, x, y, 0, 0, width, height);
            return true;
        }
        return false;
    }

    public int getLayerWidth() {
        return layerWidth;
    }

    public int getLayerHeight() {
        return layerHeight;
    }

    public boolean addMotionEvent(MotionEvent event) {
        if (getReadOnlyMode()) {
            return false;
        }
        if (shouldHandleMotionEvent(event)) {
            // Take a copy of the touch input for asynchronous processing
            motionEvents.add(MotionEvent.obtain(event));
            renderView();
            return true;
        }
        return false;
    }

    private boolean shouldHandleMotionEvent(MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        return action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE ||
                action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_POINTER_UP ||
                action == MotionEvent.ACTION_POINTER_DOWN ||
                action == MotionEvent.ACTION_CANCEL;
    }

    private boolean captureMotionEvent(MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                return motionEvent.getPointerCount() > 1;
            case MotionEvent.ACTION_POINTER_UP:
                return true;
            default:
                return false;
        }
    }

    private void processMotionEvents() {
        if (!isReady()) {
            return;
        }
        MotionEvent event;
        while ((event = motionEvents.poll()) != null) {
            if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                frontBuffer.clearFrontBufferTouches();
                persistenceThread.addEvent(createFinish());
            }

            boolean renderEvent = true;
            if (scaleDetector != null) {
                renderEvent = !captureMotionEvent(event);
                if (event.getPointerCount() > 1 && scaleDetector.onTouchEvent(event)) {
                    renderEvent = false;
                }
            }

            if (renderEvent) {
                event = transformMotionEvent(event);
                if (validateMotionEventWithLocation(event)) {
                    for (LocalWILLWriter writer : localWriters.values()) {
                        writer.startNewEvent();
                    }

                    List<WhiteboardLocalWriter.Pointer> pointers = whiteboardLocalWriter.renderLocalInputs(event, getHistoricalPointDrawingStatus(event));

                    if (event.getActionMasked() != MotionEvent.ACTION_UP && event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                        abortSnapshot();
                    }

                    frontBuffer.updateFrontBufferTouches(event);
                    if (pointers.size() > 0) {
                        persistenceThread.addEvent(createPointers(pointers));
                    }
                }
            }
            event.recycle();
        }
    }

    public View.OnTouchListener generateOnTouchListener() {
        return (view, motionEvent) -> addMotionEvent(motionEvent);
    }

    private MotionEvent lastValidMotionEvent;
    private boolean outOfScopeFlag = false;

    private MotionEvent transformMotionEvent(MotionEvent event) {
        MotionEvent transformedMotionEvent = event;
        if (hasZoomedOrPanned()) {
            transformedMotionEvent = MotionEvent.obtain(event);
            transformedMotionEvent.transform(inputMatrix);
            event.recycle();
        }
        return transformedMotionEvent;
    }

    private boolean getHistoricalPointDrawingStatus(MotionEvent motionEvent) {
        return !getMapper().outOfScope(motionEvent) && !hasZoomedOrPanned();
    }

    /*
    * Get a motionEvent from Android SDK, and do following steps:
    *   1. Is the canvas been zoomed or panned,
    *       if Yes, map the motionEvent with the offset and scaleFactor.
    *       if No, continue.
    *
    *   2. Is the mapped motionEvent out of the boundary of the canvas,
    *       if Yes, check the outOfScopeFlag,
    *           if the outOfScopeFlag == true, set isPointValid = false;
    *           if the outOfScopeFlag == false, estimate a point on the boundary, set outOfScopeFlag = true;
    *       if No, set outOfScopeFlag = false, isPointValid = true.
    *
    *   3. Check the isPointValid flag,
    *       if isPointValid = false, return.
    *       if isPointValid = true, render and sync.
    */
    private boolean validateMotionEventWithLocation(MotionEvent motionEvent) {
        boolean isPointValid;
        if (getMapper().outOfScope(motionEvent) && lastValidMotionEvent != null) {
            getMapper().estimateLocationForOutsidePoint(motionEvent, lastValidMotionEvent);
            isPointValid = !outOfScopeFlag;
            outOfScopeFlag = true;
        } else {
            lastValidMotionEvent = MotionEvent.obtain(motionEvent);
            isPointValid = true;
            outOfScopeFlag = false;
        }
        lastValidMotionEvent = MotionEvent.obtain(motionEvent);
        return isPointValid;
    }

    public void surfaceChanged(WhiteboardSurface surface, int width, int height) {
        synchronized (lock) {
            Ln.i("WhiteboardRenderer surface changed (%s, %s, %s)", surface, width, height);
            if (activeSurface.equals(surface)) {
                Ln.i("WhiteboardRenderer surface changed (%s, %s, %s) for active surface", surface, width, height);
                initCanvas(surface, width, height);
                this.width = width;
                this.height = height;

                if (activeWhiteboard != null) {
                    renderWhiteboard(activeWhiteboard);
                }

                if (onSizeChangedCallback != null) {
                    onSizeChangedCallback.onSizeChanged(width, height);
                }
            } else {
                for (PendingSurface pending : pendingSurfaceHolders) {
                    if (pending.getSurface().equals(surface)) {
                        Ln.i("WhiteboardRenderer surface changed (%s, %s, %s) for pending surface", surface, width, height);
                        pending.pendingSizeChanged(width, height);
                        return;
                    }
                }
                throw new RuntimeException(String.format("WhiteboardRenderer surface changed (%s, %s, %s) but surface is unregistered", surface, width, height));
            }
        }
    }

    public void surfaceDestroyed(WhiteboardSurface surface) {
        synchronized (lock) {
            if (activeSurface.equals(surface)) {
                Ln.i("WhiteboardRenderer surface destroyed (%s)", surface);
                releaseResources();
                whiteboardRealtimeWriter.unregister();
                activeSurface = null;
                activeWhiteboard = null;
            } else {
                boolean removed = false;
                for (int idx = 0; idx < pendingSurfaceHolders.size(); idx++) {
                    if (pendingSurfaceHolders.get(idx).getSurface().equals(surface)) {
                        Ln.i("WhiteboardRenderer surface destroyed (%s) for pending surface", surface);
                        pendingSurfaceHolders.remove(idx);
                        removed = true;
                        break;
                    }
                }
                if (!removed) {
                    throw new RuntimeException(String.format("WhiteboardRenderer surface destroyed (%s) but surface is unregistered", surface));
                }
            }
            processPendingQueue();
        }
    }

    private void processPendingQueue() {
        synchronized (lock) {
            PendingSurface pending = pendingSurfaceHolders.poll();
            if (pending != null) {
                Ln.i("WhiteboardRenderer Found Pending surface surfaceCreated(%s)", pending.getSurface());
                surfaceCreated(pending.getSurface());
                if (pending.sizeChanged()) {
                    Ln.i("WhiteboardRenderer Found Pending surface surfaceChanged (%s)", pending.getSurface());
                    surfaceChanged(pending.getSurface(), pending.getWidth(), pending.getHeight());
                }
            }
         }
    }

    public void clearWhiteboardLocal() {
        Channel currentChannel = whiteboardService.getCurrentChannel();
        boolean clearBackground = true;
        String boardId = currentChannel != null ? currentChannel.getChannelId() : null;
        Whiteboard whiteboard = whiteboardCache.getWhiteboard(boardId);
        if (whiteboard != null) {
            clearBackground = whiteboard.getBackgroundBitmap() == null;
        }

        whiteboardLocalWriter.setDrawMode(WhiteboardLocalWriter.DrawMode.NORMAL);
        resetWhiteboardLocal(clearBackground);
        renderView();
        cacheContentsIntoImage(boardId);
    }

    public void selectColor(int color) {
        whiteboardLocalWriter.selectColor(color);
    }

    public void selectEraser() {
        whiteboardLocalWriter.selectEraser();
    }

    public WhiteboardLocalWriter.DrawMode getDrawMode() {
        return whiteboardLocalWriter.getDrawMode();
    }

    public WhiteboardService getWhiteboardService() {
        return whiteboardService;
    }

    public void setReadOnlyMode(boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;
    }

    public boolean getReadOnlyMode() {
        return readOnlyMode;
    }

    public void sendRealtimeMessage(List<WhiteboardLocalWriter.Pointer> pointers) {
        List<WhiteboardLocalWriter.Pointer> contentBeginsPointers = new ArrayList<>();
        for (WhiteboardLocalWriter.Pointer pointer : pointers) {
            switch (pointer.action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    contentBeginsPointers.add(pointer);
                    break;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    whiteboardService.realtimeMessage(whiteboardRealtimeWriter.buildContentUpdateJson());
                    break;
                default:
                    break;
            }
        }

        if (!contentBeginsPointers.isEmpty()) {
            whiteboardService.realtimeMessage(whiteboardRealtimeWriter.buildContentBeginJson(contentBeginsPointers,
                    whiteboardLocalWriter.getDrawMode()));
        }
    }

    public void setOnSizeChangedCallback(OnSizeChangedCallback onSizeChangedCallback) {
        this.onSizeChangedCallback = onSizeChangedCallback;
    }

    public synchronized void addRenderListener(RenderListener renderListener) {
        renderListeners.add(renderListener);
    }

    public synchronized void removeRenderListener(RenderListener renderListener) {
        renderListeners.remove(renderListener);
    }

    public void onEventMainThread(WhiteboardLoadContentsEvent event) {
        synchronized (this) {
            for (RenderListener listener : renderListeners) {
                listener.onContentAboutToBeRendered();
            }
        }

        if (isReady()) {
            loadContents(event);

            synchronized (this) {
                for (RenderListener listener : renderListeners) {
                    listener.onContentRendered();
                }
            }
        }
    }

    public void takeSnapshotImmediate(SnapshotRunnable.OnSnapshotUploadListener onSnapshotUploadListener, boolean isSendSnapshotToChat) {
        if (isSendSnapshotToChat) {
            takeSnapshotImmediateForChat();
        } else {
            takeSnapshotImmediate(onSnapshotUploadListener);
        }
    }

    public void clear() {
        clearWhiteboardLocal();
        if (whiteboardService.getCurrentChannel() != null) {
            clearWhiteboardRemote(whiteboardRealtimeWriter.buildStartClearBoardJson());
        }
    }

    public void renderWhiteboard(Whiteboard whiteboard) {
        activeWhiteboard = whiteboard;
        if (isReady()) {
            Ln.d("WhiteboardRenderer Rendering %s", whiteboard);

            synchronized (this) {
                for (RenderListener listener : renderListeners) {
                    listener.onContentAboutToBeRendered();
                }
            }
            if (whiteboard == null) {
                Ln.e("WhiteboardRenderer Render early failed because whiteboard is null");
                return;
            }

            Ln.d("WhiteboardRenderer Rendering whiteboard with %d strokes", whiteboard.getStrokes().size());
            resetBackground();
            drawStrokes(whiteboard.getStrokes());
            Bitmap backgroundBitmap = whiteboard.getBackgroundBitmap();
            if (backgroundBitmap != null) {
                loadBitmap(backgroundBitmap);
            }
            renderView();
            cacheContentsIntoImage(whiteboard.getId());
            bus.post(new WhiteboardContentRenderedEvent(whiteboard.getId()));

            synchronized (this) {
                for (RenderListener listener : renderListeners) {
                    listener.onContentRendered();
                }
            }
        }
    }

    public float getZoomPanScale() {
        return zoomPanMatrixValues[Matrix.MSCALE_X];
    }

    private void updateMatrix(float scaleFactor, float transX, float transY) {
        Matrix matrix = new MatrixBuilder()
                .setScaleFactor(scaleFactor)
                .setTranslateX(transX)
                .setTranslateY(transY)
                .build();

        sanitizeMatrix(scaleFactor, matrix);
        updateMatrices(matrix);
    }

    private void sanitizeMatrix(float scaleFactor, Matrix matrix) {
        float[] tempArray = new float[9];
        matrix.getValues(tempArray);
        if (tempArray[MTRANS_X] > 0) {
            tempArray[MTRANS_X] = 0;
        }

        if (tempArray[MTRANS_X] < -1 * (scaleFactor - 1) * viewLayer.getWidth()) {
            tempArray[MTRANS_X] = -1 * (scaleFactor - 1) * viewLayer.getWidth();
        }

        matrix.setValues(tempArray);
    }

    public void initCanvas(WhiteboardSurface surface) {
        initCanvas(surface, 0, 0);
    }

    public void restoreInkCanvas(WhiteboardSurface surface, Whiteboard whiteboard, int width, int height) {
        Ln.d("restoreInkCanvas (%s, %s, %d, %d)", surface, whiteboard, width, height);
        if (inkCanvas == null || inkCanvas.isDisposed() || !inkCanvas.isInitialized()) {
            String reason = null;
            if (inkCanvas == null) {
                reason = "inkCanvas null";
            } else if (inkCanvas.isDisposed()) {
                reason = "inkCanvas disposed";
            } else {
                reason = "inkCanvas not initialised";
            }
            Ln.d("restoreInkCanvas (%s, %s, %d, %d) initCanvas because %s", surface, whiteboard, width, height, reason);

            initCanvas(surface, width, height);

            if (whiteboard != null) {
                Ln.d("restoreInkCanvas (%s, %s, %d, %d) renderWhiteboard", surface, whiteboard, width, height);

                renderWhiteboard(whiteboard);
            }
        }
    }

    public void initCanvas(WhiteboardSurface surface, int width, int height) {

        if (inkCanvas != null && !inkCanvas.isDisposed()) {
            // Before resetting the canvas, ensure we don't have a pending snapshot to be taken

            takeSnapshotImmediate(null);

            releaseResources();
        }

        persistenceThread = new PersistenceThread("WhiteboardPersistence");
        persistenceThread.start();

        surfaceWidth = width;
        surfaceHeight = height;

        float widthScale = surfaceWidth / WhiteboardConstants.LOGICAL_WIDTH;
        float heightScale = surfaceHeight / WhiteboardConstants.LOGICAL_HEIGHT;

        if (widthScale < heightScale) {
            scaleFactor = widthScale;
            layerWidth = surfaceWidth;
            layerHeight = (int) (scaleFactor * WhiteboardConstants.LOGICAL_HEIGHT);
            offsetX = 0;
            offsetY = (surfaceHeight - layerHeight) / 2;
        } else {
            scaleFactor = heightScale;
            layerWidth = (int) (scaleFactor * WhiteboardConstants.LOGICAL_WIDTH);
            layerHeight = surfaceHeight;
            offsetY = 0;
            offsetX = (surfaceWidth - layerWidth) / 2;
        }
        mapper = new MotionEventBoundaryMapper(layerWidth, layerHeight);
        switch (surface.getType()) {
            case SURFACETEXTURE:
                inkCanvas = InkCanvas.create(surface.getSurfaceTexture(), new EGLRenderingContext.EGLConfiguration());
                break;
            case SURFACEHOLDER:
                inkCanvas = InkCanvas.create(surface.getSurfaceHolder(), new EGLRenderingContext.EGLConfiguration());
                break;
        }
        viewLayer = inkCanvas.createViewLayer(surfaceWidth, surfaceHeight);
        strokesLayer = inkCanvas.createLayer(layerWidth, layerHeight);
        currentFrameLayer = inkCanvas.createLayer(layerWidth, layerHeight);
        pendingStrokesLayer = inkCanvas.createLayer(layerWidth, layerHeight);
        helperStrokeLayer = inkCanvas.createLayer(layerWidth, layerHeight);
        helperStrokeRenderer = new StrokeRenderer(inkCanvas, WhiteboardUtils.getDefaultStrokePaint(),
                3, helperStrokeLayer, null);
        remoteWritersLayer = inkCanvas.createLayer(layerWidth, layerHeight);
        remoteErasersLayer = inkCanvas.createLayer(layerWidth, layerHeight);
        backgroundLayer = inkCanvas.createLayer(layerWidth, layerHeight);
        resetPanAndZoom();

        clearWhiteboardLocal();
        ready = true;
        resetBackground();
        Ln.i("Whiteboard has been initiated and is ready, current inkCanvas %s", inkCanvas);
    }

    public void releaseResources() {
        ready = false;
        if (inkCanvas != null && !inkCanvas.isDisposed()) {
            inkCanvas.dispose();
            inkCanvas = null;
        }
        if (helperStrokeRenderer != null && !helperStrokeRenderer.isDisposed()) {
            helperStrokeRenderer.dispose();
        }
        for (LocalWILLWriter localWILLWriter : localWriters.values()) {
            localWILLWriter.dispose();
        }

        if (persistenceThread != null) {
            persistenceThread.addEvent(createStopThread());
            persistenceThread = null;
        }

        remoteWriters.clear();
    }

    private void resetBackground() {
        if (inkCanvas != null && inkCanvas.isInitialized()) {
            Ln.d("Reset background");
            inkCanvas.clearLayer(backgroundLayer, Color.WHITE);
        } else {
            Ln.e("Reset background failed because whiteboard inkCanvas is %s", inkCanvas == null ? "null" : "not initialised");
        }
    }

    private void loadBitmap(Bitmap bitmap) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        float scale = Math.min(
                (float) layerWidth / (float) bitmapWidth,
                (float) layerHeight / (float) bitmapHeight
        );
        int imageLayerWidth = Math.round(bitmapWidth * scale);
        int imageLayerHeight = Math.round(bitmapHeight * scale);

        float dx = (layerWidth - imageLayerWidth) * 0.5f;
        float dy = (layerHeight - imageLayerHeight) * 0.5f;
        Matrix matrix = new MatrixBuilder()
                .setTranslateX(dx)
                .setTranslateY(dy)
                .build();

        Layer imageLayer = inkCanvas.createLayer(imageLayerWidth, imageLayerHeight);
        inkCanvas.loadBitmap(imageLayer, bitmap, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE);

        inkCanvas.setTarget(backgroundLayer);
        inkCanvas.clearColor(Color.WHITE);
        inkCanvas.drawLayer(imageLayer, matrix, BlendMode.BLENDMODE_NORMAL);

        imageLayer.dispose();
    }

    public void drawStrokes(List<Stroke> strokesList) {
        //Fixme: this is a hotfix, still need to find the root cause
        if (helperStrokeRenderer == null || inkCanvas == null) {
            Ln.e("drawStrokes with helperStrokeRenderer " + helperStrokeRenderer + " inkCanvas " + inkCanvas);
            return;
        }
        try {
            helperStrokeRenderer.reset();
            for (Stroke stroke : strokesList) {
                helperStrokeRenderer.setStrokePaint(WhiteboardUtils.createStrokePaint(stroke.getColor()));
                helperStrokeRenderer.drawPoints(stroke.getScaledPointsFloatBuffer(getScaleFactor()), 0, stroke.getSize(), true);
                helperStrokeRenderer.blendStroke(strokesLayer, stroke.getBlendMode());
            }
            inkCanvas.setTarget(currentFrameLayer);
            inkCanvas.clearColor();
            inkCanvas.drawLayer(strokesLayer, zoomPanMatrix, BlendMode.BLENDMODE_NORMAL);
        } catch (Exception e) {
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
                allContents.add(whiteboardRealtimeWriter.buildContentCommitJson(writer, 0, writer.getPoints().length));
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
            inkCanvas.clearLayer(remoteErasersLayer);
        }
        if (!remoteWriters.isEmpty()) {
            helperStrokeRenderer.reset();
            for (Map.Entry<String, WILLWriter> entry : remoteWriters.entrySet()) {
                WILLWriter writer = entry.getValue();
                Layer layer = writer.getBlendMode() == BlendMode.BLENDMODE_NORMAL ?
                        remoteWritersLayer : remoteErasersLayer;
                if (isFullRedrawOfRemoteWritersNeeded) {
                    writer.renderAllSegments(helperStrokeRenderer, layer);
                } else {
                    if (writer.hasNewStrokeSegments()) {
                        writer.renderNewSegments(helperStrokeRenderer, layer);
                    }
                }
            }
            inkCanvas.setTarget(currentFrameLayer);
            inkCanvas.drawLayer(remoteWritersLayer, BlendMode.BLENDMODE_NORMAL);
            inkCanvas.drawLayer(remoteErasersLayer, BlendMode.BLENDMODE_ERASE);
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

    @Override
    public void doFrame(long l) {
        long t1 = System.nanoTime();
        processMotionEvents();

        if (inkCanvas != null && !inkCanvas.isDisposed()) {
            drawToCurrentFrameLayer();
            inkCanvas.clearLayer(viewLayer, BACKGROUND_COLOR);
            inkCanvas.drawLayer(backgroundLayer, renderMatrix, BlendMode.BLENDMODE_NORMAL);
            inkCanvas.drawLayer(currentFrameLayer, renderMatrix, BlendMode.BLENDMODE_NORMAL);
            inkCanvas.invalidate();

            frameCallbackPosted = false;
        } else {
            Choreographer.getInstance().postFrameCallback(this);
            frameCallbackPosted = true;
        }
        long t5 = System.nanoTime();
    }

    public void renderView() {
        if (!frameCallbackPosted) {
            Choreographer.getInstance().postFrameCallback(this);
            frameCallbackPosted = true;
        }
    }

    public void renderContents(List<Content> contents) {
        renderContents(contents, null);
    }

    public void renderContents(List<Content> contents, String remoteWriterKey) {
        renderContents(contents, whiteboardService.getBoardId(), whiteboardCache, remoteWriterKey);
    }

    public void renderContents(List<Content> contents, String cacheBoardId, final WhiteboardCache whiteboardCache, final String remoteWriterKey) {
        boolean isCacheInitialized = whiteboardCache.isCacheValidForBoard(cacheBoardId);
        processContentPayload(contents)
                .subscribe(strokes -> {
                    drawStrokes(strokes);
                    cacheStrokes(cacheBoardId, whiteboardCache, isCacheInitialized, strokes);
                    if (remoteWriterKey != null) {
                        removeRemoteWriter(remoteWriterKey);
                    }
                    renderView();
                    cacheContentsIntoImage(cacheBoardId);
                }, Ln::e);
    }

    private void cacheStrokes(String cacheBoardId, WhiteboardCache whiteboardCache, boolean cacheInited, List<Stroke> strokes) {
        if (!TextUtils.isEmpty(cacheBoardId) && !cacheInited) {
            whiteboardCache.initAndStartRealtimeForBoard(cacheBoardId, strokes);
        }
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
                && CURVE_TYPE.equalsIgnoreCase(
                parsedContent.get(CONTENT_TYPE).getAsString());
    }

    private JsonObject parsePayload(Content content) {

        if (content == null || content.getPayload() == null)
            return null;

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

    private Stroke generateStrokeByContent(JsonObject payload) {
        if (payload == null) return null;
        return isCurve(payload) ? WhiteboardUtils.createStroke(payload, gson) : null;
    }

    public void persistFinishedStrokes() {
        for (Map.Entry<UUID, LocalWILLWriter> entry : writersBeingSaved.entrySet()) {
            LocalWILLWriter writer = entry.getValue();
            if (writer != null && !writer.isSaving() && !writer.isSaved()) {
                writer.setSaving(true);
                whiteboardCache.addStrokeToCurrentRealtimeBoard(
                        WhiteboardUtils.createStroke(writer, getScaleFactor()));
                List<Content> contentRequests = new ArrayList<>();
                splitUpCurve(writer, (sameWriter, s, end) -> contentRequests
                        .add(buildStrokeContent(whiteboardRealtimeWriter.buildContentCommitJson(sameWriter, s, end))));

                postStroke(contentRequests, writer.getWriterId());
            }
        }
    }

    public void splitUpCurve(LocalWILLWriter writer, SplitUpCurveCallback callback) {
        int index = 0;
        int pointsLimit = WHITEBOARD_CONTENT_POINTS_LIMIT * writer.getStride(); // 1000 points
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

    public void resetWhiteboardLocal(boolean clearBackground) {
        Ln.d("resetWhiteboardLocal(%s)", clearBackground);

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
                inkCanvas.clearLayer(remoteErasersLayer);
                inkCanvas.clearLayer(currentFrameLayer);
                if (clearBackground) {
                    inkCanvas.clearLayer(backgroundLayer, Color.WHITE);
                }
            } catch (NullPointerException e) {
                Ln.w(e);
            }
        } else {
            Ln.e("resetWhiteboardLocal(%s) inkCanvas is null", clearBackground);
        }
    }

    public void clearWhiteboardRemote(JsonObject clearBoardJson) {
        if (whiteboardService.getBoardId() != null) {
            whiteboardService.realtimeMessage(clearBoardJson);
        }

        whiteboardService.clearBoard();
        takeSnapshotDelayed();
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
        if (pointerId < 0 || pointerId >= maxTouchPointers) {
            throw new IllegalArgumentException("pointerId must be between 0 and WhiteboardConstants.maxTouchPointers");
        }

        return localWriters.get(pointerId);
    }

    public Map<Integer, LocalWILLWriter> getLocalWriters() {
        return localWriters;
    }

    public Map<UUID, LocalWILLWriter> getWritersBeingSaved() {
        return writersBeingSaved;
    }

    public LocalWILLWriter createNewLocalWILLWriter(int drawColor, BlendMode strokeBlendMode) {
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
            LayerPool.ReusableLayer strokeLayer = layerPool.obtainLayer(inkCanvas, layerWidth, layerHeight);
            LayerPool.ReusableLayer strokeWithPreliminaryLayer = layerPool.obtainLayer(inkCanvas, layerWidth, layerHeight);
            inkCanvas.setTarget(currentFrameLayer);
            return new LocalWILLWriter(drawColor, strokeBlendMode, inkCanvas, strokeLayer, strokeWithPreliminaryLayer, scaleFactor);
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
        removeLocalWriter(pointerId);
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

    WILLWriter getRemoteWriter(String remoteWriterKey) {
        return remoteWriters.get(remoteWriterKey);
    }

    void commitRemoteWriter(String remoteWriterKey, List<Content> contentsList) {

        renderContents(contentsList, remoteWriterKey);
    }

    void cancelRemoteWriter(String remoteWriterKey) {

        removeRemoteWriter(remoteWriterKey);
        renderView();
    }

    private void loadContents(WhiteboardLoadContentsEvent event) {
        if (event.shouldResetBoard()) {
            resetWhiteboardLocal(true);
        }

        renderContents(event.getContents(), event.getBoardId(), whiteboardCache, null);
    }

    public void cacheContentsIntoImage(String boardId) {
        if (TextUtils.isEmpty(boardId)) {
            Ln.e("cacheContentsIntoImage: boardId is empty");
            return;
        }

        try {
            Ln.i("Cached Content snapshot refreshed");
            Whiteboard cachedWhiteboard = whiteboardCache.getWhiteboard(boardId);
            if (cachedWhiteboard != null) {
                Bitmap bitmap = getSnapshot();
                if (bitmap != null) {
                    cachedWhiteboard.setCachedSnapshot(bitmap);
                }
            }
        } catch (Exception e) {
            Ln.e(e);
        }
    }

    private Bitmap getSnapshot() {
        if (inkCanvas == null || inkCanvas.isDisposed()) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);
        if (bitmap == null) {
            return null;
        }

        inkCanvas.setTarget(helperStrokeLayer);
        inkCanvas.drawLayer(backgroundLayer, BlendMode.BLENDMODE_NORMAL);
        inkCanvas.drawLayer(currentFrameLayer, BlendMode.BLENDMODE_NORMAL);
        inkCanvas.readPixels(helperStrokeLayer, bitmap, 0, 0, 0, 0, layerWidth, layerHeight);

        return bitmap;
    }

    private void takeSnapshotDelayed() {
        if (snapShotHandler == null) {
            return;
        }
        abortSnapshot();
        SnapshotRunnable snapshotRunnable = new SnapshotRunnable(whiteboardService, surfaceWidth, surfaceHeight, inkCanvas, viewLayer, bus, context);
        snapshotRunnable.setChannel(whiteboardService.getCurrentChannel());
        snapshotRunnable.setChannelCreationRequestId(channelCreationRequestId);
        synchronized (snapShotHandler) {
            snapShotHandler.sendMessageDelayed(Message.obtain(snapShotHandler, SNAPSHOT_RUNNABLE_PENDING, snapshotRunnable), SNAPSHOT_UPLOAD_DELAY_MILLIS);
        }
    }

    private void takeSnapshotImmediate(final SnapshotRunnable.OnSnapshotUploadListener onSnapshotUploadListener) {
        if (snapShotHandler == null) {
            return;
        }
        synchronized (snapShotHandler) {
            // Only take snapshot at once if a delayed one was in the queue
            if (snapShotHandler.hasMessages(SNAPSHOT_RUNNABLE_PENDING)) {
                abortSnapshot();
                SnapshotRunnable snapshotRunnable = new SnapshotRunnable(whiteboardService, surfaceWidth, surfaceHeight, inkCanvas, viewLayer, bus, context);
                snapshotRunnable.setChannel(whiteboardService.getCurrentChannel());
                snapshotRunnable.setChannelCreationRequestId(channelCreationRequestId);
                snapshotRunnable.setOnSnapshotUploadListener(onSnapshotUploadListener);
                Message message = Message.obtain(snapShotHandler, SNAPSHOT_RUNNABLE_PENDING, snapshotRunnable);
                snapShotHandler.handleMessage(message);
            } else if (onSnapshotUploadListener != null) {
                onSnapshotUploadListener.onSyncUIWorkComplete();
            }
        }
    }

    private void takeSnapshotImmediateForChat() {
        if (snapShotHandler != null) {
            synchronized (snapShotHandler) {
                SendToChatSnapshotRunnable sendToChatSnapshotRunnable = new SendToChatSnapshotRunnable(whiteboardService, context, whiteboardService.getCurrentChannel(), bus, whiteboardCache);
                snapShotHandler.sendMessage(Message.obtain(snapShotHandler, SNAPSHOT_RUNNABLE_PENDING, sendToChatSnapshotRunnable));
            }
        }
    }

    void abortSnapshot() {
        if (snapShotHandler == null) {
            return;
        }
        synchronized (snapShotHandler) {
            snapShotHandler.removeMessages(SNAPSHOT_RUNNABLE_PENDING);
        }
    }

    boolean isReady() {
        return ready;
    }

    void removeLocalWriter(int pointerId) {
        LocalWILLWriter writer = localWriters.remove(pointerId);
        if (writer == null) {
            Ln.w("Tried to remove a non existing writer for pointer ID " + pointerId);
            return;
        }
        writer.dispose();
    }

    public void resetPanAndZoom() {
        updateMatrix(1f, 0f, 0f);
    }

    private void setOffset(float offsetX, float offsetY) {
        float scaleFactor = getZoomPanScale();
        Matrix matrix = new MatrixBuilder()
                .setScaleFactor(scaleFactor)
                .setTranslateX(offsetX)
                .setTranslateY(offsetY)
                .build();
        updateMatrices(matrix);

    }

    private void updateMatrices(Matrix matrix) {
        zoomPanMatrix = matrix;
        zoomPanMatrix.getValues(zoomPanMatrixValues);

        renderMatrix.set(matrix);
        renderMatrix.preTranslate(offsetX, offsetY);

        renderMatrix.invert(inputMatrix);
    }

    public boolean hasZoomedOrPanned() {
        return !inputMatrix.isIdentity();
    }

    public MotionEventBoundaryMapper getMapper() {
        return mapper;
    }

    private static class SnapshotHandler extends Handler {

        public SnapshotHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SNAPSHOT_RUNNABLE_PENDING) {
                Runnable runnable = (Runnable) msg.obj;
                runnable.run();
            }
        }
    }

    public interface SplitUpCurveCallback {
        void execute(LocalWILLWriter writer, int start, int end);
    }

    public void renderImage(Bitmap bitmap) {
        resetWhiteboardLocal(true);
        inkCanvas.clearLayer(currentFrameLayer, Color.WHITE);
        inkCanvas.setTarget(currentFrameLayer);
        inkCanvas.loadBitmap(strokesLayer, bitmap, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE);
        renderView();
    }

    public void setChannelCreationRequestId(UUID channelCreationRequestId) {
        this.channelCreationRequestId = channelCreationRequestId;
    }

    public interface OnSizeChangedCallback {
        void onSizeChanged(int width, int height);
    }

    public interface RenderListener {
        void onContentRendered();
        void onContentAboutToBeRendered();
    }

    private static class PendingSurface {
        private final WhiteboardSurface whiteboardSurface;
        private int width;
        private int height;
        private boolean gotSizeChanged;

        public PendingSurface(WhiteboardSurface surfaceHolder) {
            this.whiteboardSurface = surfaceHolder;
        }

        public void pendingSizeChanged(int width, int height) {
            gotSizeChanged = true;
            this.width  = width;
            this.height = height;
        }

        WhiteboardSurface getSurface() {
            return whiteboardSurface;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean sizeChanged() {
            return gotSizeChanged;
        }
    }

}
