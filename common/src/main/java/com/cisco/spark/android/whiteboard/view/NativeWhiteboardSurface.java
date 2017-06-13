package com.cisco.spark.android.whiteboard.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardLoadContentsEvent;
import com.cisco.spark.android.whiteboard.view.writer.WhiteboardLocalWriter;
import com.cisco.spark.android.whiteboard.view.writer.WhiteboardRealtimeWriter;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public abstract class NativeWhiteboardSurface extends SurfaceView {

    protected WhiteboardController whiteboardController;
    protected WhiteboardLocalWriter whiteboardLocalWriter;
    protected WhiteboardRealtimeWriter whiteboardRealtimeWriter;

    @Inject protected WhiteboardService whiteboardService;
    @Inject ApiTokenProvider apiTokenProvider;
    @Inject SdkClient sdkClient;
    @Inject EventBus bus;
    @Inject Gson gson;
    @Inject SchedulerProvider schedulerProvider;
    @Inject WhiteboardCache whiteboardCache;

    protected boolean readOnlyMode;

    private OnSizeChangedCallback onSizeChangedCallback;
    private OnContentAboutToBeRendered onContentAboutToBeRendered;

    private static Runnable surfaceReleasedRunnable;
    private Whiteboard loadWhiteboardWhenReady;

    public NativeWhiteboardSurface(Context context, Injector injector) {
        this(context, null, injector);
    }

    public NativeWhiteboardSurface(Context context, AttributeSet attrs, Injector injector) {
        this(context, attrs, 0, injector);
    }

    public NativeWhiteboardSurface(Context context, AttributeSet attrs, int defStyleAttr, Injector injector) {
        super(context, attrs, defStyleAttr);

        create(injector);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NativeWhiteboardSurface(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes,
                                   Injector injector) {
        super(context, attrs, defStyleAttr, defStyleRes);

        create(injector);
    }

    public void create(Injector injector) {

        injector.inject(this);

        whiteboardController = new WhiteboardController(gson, sdkClient, whiteboardService, this, bus, schedulerProvider, whiteboardCache);
        whiteboardLocalWriter = new WhiteboardLocalWriter(whiteboardController);
        whiteboardRealtimeWriter = new WhiteboardRealtimeWriter(whiteboardController, gson, bus, apiTokenProvider,
                                                                whiteboardService, whiteboardCache);

        getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

                Ln.i("Whiteboard surface created (%s)", surfaceHolder);
                whiteboardRealtimeWriter.register();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

                Ln.i("Whiteboard surface changed (%s, %s, %s, %s)", surfaceHolder, format, width, height);
                whiteboardController.initCanvas(surfaceHolder, width, height);

                if (loadWhiteboardWhenReady != null) {
                    loadWhiteboard(loadWhiteboardWhenReady);
                    loadWhiteboardWhenReady = null;
                }

                if (onSizeChangedCallback != null) {
                    onSizeChangedCallback.onSizeChanged(width, height);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

                Ln.i("Whiteboard surface destroyed (%s)", surfaceHolder);
                whiteboardController.releaseResources();
                whiteboardRealtimeWriter.unregister();

                if (surfaceReleasedRunnable != null) {
                    surfaceReleasedRunnable.run();
                    surfaceReleasedRunnable = null;
                }
            }
        });

        setOnTouchListener(generateOnTouchListener());
    }

    public abstract OnTouchListener generateOnTouchListener();

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        bus.register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        bus.unregister(this);
    }

    public void clearWhiteboardLocal() {
        whiteboardLocalWriter.clearWhiteboardLocal();
    }

    public void selectColor(int color) {
        whiteboardLocalWriter.selectColor(color);
    }

    public void selectEraser() {
        whiteboardLocalWriter.selectEraser();
    }

    public WhiteboardController getWhiteboardController() {
        return whiteboardController;
    }

    public WhiteboardLocalWriter getWhiteboardLocalWriter() {
        return whiteboardLocalWriter;
    }

    public void setReadOnlyMode(boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;
    }

    protected void sendRealtimeMessage(WhiteboardLocalWriter.Pointer changedPointer) {
        switch (changedPointer.action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                whiteboardService.realtimeMessage(whiteboardRealtimeWriter.buildContentBeginJson(changedPointer,
                                                                                                 whiteboardLocalWriter.getDrawMode()));
                break;
            case MotionEvent.ACTION_MOVE:
                whiteboardService.realtimeMessage(whiteboardRealtimeWriter.buildContentUpdateJson());
                break;
            default:
                break;
        }
    }

    protected boolean shouldHandleMotionEvent(MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        return action != MotionEvent.ACTION_DOWN &&
               action != MotionEvent.ACTION_MOVE &&
               action != MotionEvent.ACTION_UP &&
               action != MotionEvent.ACTION_POINTER_DOWN &&
               action != MotionEvent.ACTION_POINTER_UP;
    }

    public void setOnSizeChangedCallback(OnSizeChangedCallback onSizeChangedCallback) {
        this.onSizeChangedCallback = onSizeChangedCallback;
    }

    public void setOnContentAboutToBeRendered(OnContentAboutToBeRendered onContentAboutToBeRendered) {
        this.onContentAboutToBeRendered = onContentAboutToBeRendered;
    }

    public void onEventMainThread(WhiteboardLoadContentsEvent event) {
        if (onContentAboutToBeRendered != null) {
            onContentAboutToBeRendered.onContentAboutToBeRendered();
        }

        if (whiteboardController.isReady()) {
            whiteboardController.loadContents(event);
        }
    }

    public static void setSurfaceReleasedRunnable(Runnable runnable) {
        surfaceReleasedRunnable = runnable;
    }

    public void takeSnapshotImmediate(SnapshotRunnable.OnSnapshotUploadListener onSnapshotUploadListener, boolean isSendSnapshotToChat) {
        if (isSendSnapshotToChat) {
            whiteboardController.takeSnapshotImmediateForChat();
        } else {
            whiteboardController.takeSnapshotImmediate(onSnapshotUploadListener);
        }
    }

    public void clear() {
        clearWhiteboardLocal();
        if (whiteboardService.getCurrentChannel() != null) {
            whiteboardController.clearWhiteboardRemote(whiteboardRealtimeWriter.buildStartClearBoardJson());
        }
    }

    public void loadWhiteboard(Whiteboard whiteboard) {
        if (whiteboardController.isReady()) {
            Ln.d("Loading %s from cache", whiteboard);

            if (onContentAboutToBeRendered != null) {
                onContentAboutToBeRendered.onContentAboutToBeRendered();
            }

            whiteboardController.loadWhiteboard(whiteboard);

        } else {
            loadWhiteboardWhenReady = whiteboard;
        }
    }

    public interface OnSizeChangedCallback {
        void onSizeChanged(int width, int height);
    }

    public interface OnContentAboutToBeRendered {

        void onContentAboutToBeRendered();
    }

    public void releaseInkResource() {
        whiteboardController.releaseResources();
    }
}
