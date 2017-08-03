package com.cisco.spark.android.whiteboard.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextUtils;

import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardSendSnapshotToChatEvent;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;

import java.io.File;

import de.greenrobot.event.EventBus;

public class SendToChatSnapshotRunnable implements Runnable {

    protected final WhiteboardCache whiteboardCache;
    protected final Context context;
    protected final WhiteboardService whiteboardService;
    protected final EventBus bus;
    protected Channel channel;

    public SendToChatSnapshotRunnable(WhiteboardService whiteboardService, Context context, Channel channel, EventBus bus, WhiteboardCache whiteboardCache) {
        this.whiteboardCache = whiteboardCache;
        this.whiteboardService = whiteboardService;
        this.channel = channel;
        this.context = context;
        this.bus = bus;
    }

    @Override
    public void run() {

        if (channel == null) { // May have been null if runnable created right after new board created
            channel = whiteboardService.getCurrentChannel();
        }

        if (channel == null || TextUtils.isEmpty(channel.getChannelId())) return;
        Whiteboard cache = whiteboardCache.getWhiteboard(channel.getChannelId());
        if (cache != null) {
            Bitmap snapshot = cache.getCachedSnapshot();
            dealWithSnapShot(snapshot, generateFilePath());
        }
    }

    protected void dealWithSnapShot(Bitmap scaledBitmap, String filePath) {
        File file = saveSnapshot(compositeWhiteBackground(scaledBitmap), filePath);
        if (file != null) {
            bus.post(new WhiteboardSendSnapshotToChatEvent(file));
        }
    }

    protected Bitmap compositeWhiteBackground(Bitmap bitmap) {
        Bitmap newbmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newbmp);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(newbmp, WhiteboardConstants.SNAPSHOT_WIDTH, WhiteboardConstants.SNAPSHOT_HEIGHT, true);
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        newbmp.recycle();
        return scaledBitmap;
    }

    protected String generateFilePath() {
        return context.getCacheDir() + "//" + WhiteboardConstants.WB_SNAPSHOT_FILENAME_BASE +
                "_" + channel.getChannelId() + WhiteboardConstants.WB_SNAPSHOT_FILENAME_EXTENSION;
    }

    protected File saveSnapshot(Bitmap scaledBitmap, String filePath) {
        synchronized (scaledBitmap) {
            File file = new File(filePath);
            if (ImageUtils.writeBitmap(file, scaledBitmap)) {
                return file;
            } else {
                return null;
            }
        }
    }
}
