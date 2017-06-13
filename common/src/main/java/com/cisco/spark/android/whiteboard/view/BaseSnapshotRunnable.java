package com.cisco.spark.android.whiteboard.view;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.SurfaceView;

import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;

import java.io.File;
import java.lang.ref.WeakReference;

import de.greenrobot.event.EventBus;


abstract public class BaseSnapshotRunnable implements Runnable {

    protected final WhiteboardService whiteboardService;
    protected final EventBus bus;
    protected WeakReference<SurfaceView> surfaceViewRef;
    protected WeakReference<InkCanvas> inkCanvasRef;
    protected WeakReference<Layer> viewLayerRef;
    protected Channel channel;
    protected final OnSnapshotUploadListener onSnapshotUploadListener;

    public BaseSnapshotRunnable(WhiteboardService whiteboardService, SurfaceView surfaceView, InkCanvas inkCanvas,
                                Layer viewLayer, Channel channel, OnSnapshotUploadListener onSnapshotUploadListener, EventBus bus) {
        this.whiteboardService = whiteboardService;
        this.surfaceViewRef = new WeakReference<>(surfaceView);
        this.inkCanvasRef = new WeakReference<>(inkCanvas);
        this.viewLayerRef = new WeakReference<>(viewLayer);
        this.channel = channel;
        this.onSnapshotUploadListener = onSnapshotUploadListener;
        this.bus = bus;
    }

    @Override
    public void run() {

        SurfaceView surfaceView = surfaceViewRef.get();
        InkCanvas inkCanvas = inkCanvasRef.get();
        Layer viewLayer = viewLayerRef.get();

        if (channel == null) { // May have been null if runnable created right after new board created
            channel = whiteboardService.getCurrentChannel();
        }

        if (inkCanvas != null && inkCanvas.isInitialized() && !inkCanvas.isDisposed() && viewLayer != null &&
                channel != null && surfaceView != null) {

            Bitmap bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(),
                    Bitmap.Config.ARGB_8888);

            inkCanvas.readPixels(viewLayer, bitmap, 0, 0, 0, 0, surfaceView.getWidth(), surfaceView.getHeight());
            if (onSnapshotUploadListener != null) {
                onSnapshotUploadListener.onSyncUIWorkComplete();
            }

            Bitmap scaledBitmap = getScaledBitmap(bitmap);
            dealWithSnapShot(scaledBitmap, surfaceView);

            bitmap.recycle();
            scaledBitmap.recycle();
        }
    }

    protected void uploadSnapshot(String filePath, final Channel channelForUpload) {
        File f = new File(filePath);
        if (!f.exists() || f.isDirectory()) {
            return;
        }
        Channel currentChannel = whiteboardService.getCurrentChannel();
        if (currentChannel == null || Strings.equals(currentChannel.getChannelId(), channelForUpload.getChannelId())) {
            whiteboardService.uploadWhiteboardSnapshot(Uri.parse("file://" + f.getAbsolutePath()), channelForUpload);
        }
    }

    public interface OnSnapshotUploadListener {
        void onSyncUIWorkComplete();
    }

    protected abstract Bitmap getScaledBitmap(Bitmap bitmap);

    protected abstract void dealWithSnapShot(Bitmap scaledBitmap, SurfaceView surfaceView);

}
