package com.cisco.spark.android.whiteboard.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;

import com.cisco.spark.android.core.RootModule;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.whiteboard.WhiteboardChannelImageBitmapChangedEvent;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.snapshot.SnapshotManager;
import com.cisco.spark.android.whiteboard.util.FileUtilities;
import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.UUID;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SNAPSHOT_HEIGHT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SNAPSHOT_WIDTH;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WB_SNAPSHOT_FILENAME_BASE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WB_SNAPSHOT_FILENAME_EXTENSION;

public class SnapshotRunnable implements Runnable {

    @Inject SnapshotManager mSnapshotManager;

    protected final WhiteboardService whiteboardService;
    protected final EventBus bus;
    protected WeakReference<InkCanvas> inkCanvasRef;
    protected WeakReference<Layer> viewLayerRef;
    private final int width;
    private final int height;

    private Channel channel;
    private UUID channelCreationRequestId;
    private OnSnapshotUploadListener onSnapshotUploadListener;

    public SnapshotRunnable(WhiteboardService whiteboardService, int width, int height, InkCanvas inkCanvas, Layer viewLayer, EventBus bus, Context context) {
        this.whiteboardService = whiteboardService;
        this.width = width;
        this.height = height;
        this.inkCanvasRef = new WeakReference<>(inkCanvas);
        this.viewLayerRef = new WeakReference<>(viewLayer);
        this.bus = bus;

        RootModule.getInjector().inject(this);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setChannelCreationRequestId(UUID channelCreationRequestId) {
        this.channelCreationRequestId = channelCreationRequestId;
    }

    public void setOnSnapshotUploadListener(OnSnapshotUploadListener onSnapshotUploadListener) {
        this.onSnapshotUploadListener = onSnapshotUploadListener;
    }

    @Override
    public void run() {
        InkCanvas inkCanvas = inkCanvasRef.get();
        Layer viewLayer = viewLayerRef.get();

        if (inkCanvas != null && inkCanvas.isInitialized() && !inkCanvas.isDisposed()
                && viewLayer != null) {

            Bitmap bitmap = Bitmap.createBitmap(width, height, ARGB_8888);
            inkCanvas.readPixels(viewLayer, bitmap, 0, 0, 0, 0, width, height);
            if (onSnapshotUploadListener != null) {
                onSnapshotUploadListener.onSyncUIWorkComplete();
            }

            Bitmap scaledBitmap = getScaledBitmap(bitmap);
            bitmap.recycle();

            if (scaledBitmap != null) {
                processSnapshot(scaledBitmap);
                scaledBitmap.recycle();
            }
        }
    }

    protected String filePath(String id) {
        return mSnapshotManager.getSnapshotUploadDir() + WB_SNAPSHOT_FILENAME_BASE + "_" + id + WB_SNAPSHOT_FILENAME_EXTENSION;
    }

    private Bitmap getScaledBitmap(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT, true);
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        return scaledBitmap;
    }

    private void processSnapshot(Bitmap scaledBitmap) {
        if (channel != null) {
            String filePath = filePath(channel.getChannelId());
            File file = new File(filePath);
            if (ImageUtils.writeBitmap(file, scaledBitmap)) {
                bus.post(new WhiteboardChannelImageBitmapChangedEvent(channel, FileUtilities.getByteArray(scaledBitmap, true)));
                whiteboardService.uploadWhiteboardSnapshot(Uri.parse("file://" + file.getAbsolutePath()), channel);
            }
        } else if (channelCreationRequestId != null) {
            String filePath = filePath(channelCreationRequestId.toString());
            File file = new File(filePath);
            if (ImageUtils.writeBitmap(file, scaledBitmap)) {
                whiteboardService.uploadWhiteboardSnapshot(Uri.parse("file://" + file.getAbsolutePath()), channelCreationRequestId);
            }
        }
    }

    public interface OnSnapshotUploadListener {
        void onSyncUIWorkComplete();
    }
}
