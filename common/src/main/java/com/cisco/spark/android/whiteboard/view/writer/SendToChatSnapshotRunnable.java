package com.cisco.spark.android.whiteboard.view.writer;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.view.SurfaceView;

import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.whiteboard.WhiteboardSendSnapshotToChatEvent;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.view.BaseSnapshotRunnable;
import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;

import java.io.File;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SNAPSHOT_HEIGHT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SNAPSHOT_WIDTH;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WB_SNAPSHOT_FILENAME_EXTENSION;

public class SendToChatSnapshotRunnable extends BaseSnapshotRunnable {

    public SendToChatSnapshotRunnable(WhiteboardService whiteboardService, SurfaceView surfaceView, InkCanvas inkCanvas,
                                      Layer viewLayer, Channel channel, OnSnapshotUploadListener onSnapshotUploadListener, EventBus bus) {
        super(whiteboardService, surfaceView, inkCanvas,
                viewLayer, channel, onSnapshotUploadListener, bus);
    }

    @Override
    protected Bitmap getScaledBitmap(Bitmap bitmap) {
        Bitmap newbmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newbmp);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(newbmp, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT, true);
        Matrix matrix = new Matrix();
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(),
                matrix, true);
        newbmp.recycle();
        return scaledBitmap;
    }

    @Override
    protected void dealWithSnapShot(Bitmap scaledBitmap, SurfaceView surfaceView) {
        // FIXME Dubious
        String filePath = surfaceView.getContext().getCacheDir() + "//" + WhiteboardConstants.WB_SNAPSHOT_FILENAME_BASE +
                "_" + channel.getChannelId() + WB_SNAPSHOT_FILENAME_EXTENSION;

        File file = new File(filePath);
        if (ImageUtils.writeBitmap(file, scaledBitmap)) {
            bus.post(new WhiteboardSendSnapshotToChatEvent(file));
        }
    }
}
