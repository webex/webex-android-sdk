package com.cisco.spark.android.whiteboard.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer;
import com.cisco.spark.android.whiteboard.renderer.WhiteboardSurface;

import javax.inject.Inject;

public class NativeWhiteboardSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    @Inject protected WhiteboardRenderer whiteboardRenderer;

    public NativeWhiteboardSurfaceView(Context context, Injector injector) {
        this(context, null, injector);
    }

    public NativeWhiteboardSurfaceView(Context context, AttributeSet attrs, Injector injector) {
        this(context, attrs, 0, injector);
    }

    public NativeWhiteboardSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, Injector injector) {
        super(context, attrs, defStyleAttr);

        create(injector);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NativeWhiteboardSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes,
                                       Injector injector) {
        super(context, attrs, defStyleAttr, defStyleRes);

        create(injector);
    }

    public void create(Injector injector) {

        injector.inject(this);

        getHolder().addCallback(this);

        setOnTouchListener(whiteboardRenderer.generateOnTouchListener());
    }

    @Override
    public final void surfaceCreated(SurfaceHolder surfaceHolder) {
        whiteboardRenderer.surfaceCreated(new WhiteboardSurface(surfaceHolder));
    }

    @Override
    public final void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        whiteboardRenderer.surfaceChanged(new WhiteboardSurface(surfaceHolder), width, height);
    }

    @Override
    public final void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        whiteboardRenderer.surfaceDestroyed(new WhiteboardSurface(surfaceHolder));
    }
}
