package com.cisco.spark.android.whiteboard.view;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.SurfaceView;

import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.whiteboard.WhiteboardChannelImageBitmapChangedEvent;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.util.FileUtilities;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;

import java.io.File;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SNAPSHOT_HEIGHT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SNAPSHOT_WIDTH;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WB_SNAPSHOT_FILENAME_EXTENSION;

public class SnapshotRunnable extends BaseSnapshotRunnable {

    public SnapshotRunnable(WhiteboardService whiteboardService, SurfaceView surfaceView, InkCanvas inkCanvas,
                            Layer viewLayer, Channel channel, OnSnapshotUploadListener onSnapshotUploadListener, EventBus bus) {
        super(whiteboardService, surfaceView, inkCanvas,
                viewLayer, channel, onSnapshotUploadListener, bus);
    }

    @Override
    protected Bitmap getScaledBitmap(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT, true);
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(),
                matrix, true);
        return scaledBitmap;
    }

    @Override
    protected void dealWithSnapShot(Bitmap scaledBitmap, SurfaceView surfaceView) {
        // FIXME Dubious
        String filePath = surfaceView.getContext().getCacheDir() + "//" + WhiteboardConstants.WB_SNAPSHOT_FILENAME_BASE +
                "_" + channel.getChannelId() + WB_SNAPSHOT_FILENAME_EXTENSION;

        File file = new File(filePath);
        if (ImageUtils.writeBitmap(file, scaledBitmap)) {
            bus.post(new WhiteboardChannelImageBitmapChangedEvent(channel, FileUtilities.getByteArray(scaledBitmap)));
            uploadSnapshot(filePath, channel);
        }
    }
}
