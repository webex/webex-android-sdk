package com.cisco.spark.android.whiteboard.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.TextureView;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.whiteboard.renderer.WhiteboardRenderer;
import com.cisco.spark.android.whiteboard.renderer.WhiteboardSurface;

import javax.inject.Inject;

public abstract class NativeWhiteboardTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    @Inject protected WhiteboardRenderer whiteboardRenderer;

    public NativeWhiteboardTextureView(Context context, Injector injector) {
        this(context, null, injector);
    }

    public NativeWhiteboardTextureView(Context context, AttributeSet attrs, Injector injector) {
        this(context, attrs, 0, injector);
    }

    public NativeWhiteboardTextureView(Context context, AttributeSet attrs, int defStyleAttr, Injector injector) {
        super(context, attrs, defStyleAttr);

        create(injector);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NativeWhiteboardTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes,
                                       Injector injector) {
        super(context, attrs, defStyleAttr, defStyleRes);

        create(injector);
    }

    public void create(Injector injector) {

        injector.inject(this);

        setSurfaceTextureListener(this);

        setOnTouchListener(whiteboardRenderer.generateOnTouchListener());
    }

    @Override
    public final void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        whiteboardRenderer.surfaceCreated(new WhiteboardSurface(surfaceTexture));
        whiteboardRenderer.surfaceChanged(new WhiteboardSurface(surfaceTexture), width, height);
    }

    @Override
    public final void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        whiteboardRenderer.surfaceChanged(new WhiteboardSurface(surfaceTexture), width, height);
    }

    @Override
    public final boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        whiteboardRenderer.surfaceDestroyed(new WhiteboardSurface(surfaceTexture));
        return true;
    }

    @Override
    public final void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
}
