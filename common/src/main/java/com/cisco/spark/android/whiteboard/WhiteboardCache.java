package com.cisco.spark.android.whiteboard;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.LruCache;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardRealtimeEvent;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.greenrobot.event.EventBus;

public class WhiteboardCache implements Component {

    private static final int CACHE_SIZE_LIMIT = 5;

    private final EventBus eventBus;
    private final Gson gson;
    private final LruCache<String, Whiteboard> cache;
    private List<Stroke> bufferedStrokes;
    private String listeningToBoard;
    private boolean isBufferingRealtimeStrokes;

    public WhiteboardCache(EventBus eventBus, Gson gson) {

        this.eventBus = eventBus;
        this.gson = gson;

        cache = new LruCache<>(CACHE_SIZE_LIMIT);
        bufferedStrokes = new ArrayList<>();
    }

    public boolean isCacheValidForBoard(String boardId) {
        return !TextUtils.isEmpty(listeningToBoard) && listeningToBoard.equals(boardId);
    }

    @Nullable
    public synchronized Whiteboard getWhiteboard(@Nullable String boardId) {

        if (boardId == null) {
            Ln.e("input a invalid boardId");
            return null;
        }

        Whiteboard wb = cache.get(boardId);
        if (wb != null) {
            Ln.d("Retrieving board with %d strokes, stale: %b", wb.getStrokes().size(), wb.isStale());
        } else {
            Ln.d("No board found");
        }
        return wb;
    }

    public synchronized void initAndStartRealtimeForBoard(String whiteboardId, List<Stroke> strokesList) {
        Ln.v("[CacheTest] init cache with id = %s", whiteboardId);
        prepareRealtimeForBoard(whiteboardId);
        createAndAddBoardToCache(whiteboardId, strokesList);
    }

    public synchronized void prepareRealtimeForBoard(String whiteboardId) {
        markAllBoardsAsStale();
        this.listeningToBoard = whiteboardId;
        this.bufferedStrokes.clear();
        this.isBufferingRealtimeStrokes = true;
    }

    public synchronized Whiteboard createAndAddBoardToCache(String whiteboardId, List<Stroke> strokesList) {
        return createAndAddBoardToCache(whiteboardId, strokesList, null, null);
    }

    public synchronized Whiteboard createAndAddBoardToCache(String whiteboardId, List<Stroke> strokesList, Content backgroundContent, Bitmap backgroundBitmap) {
        Ln.d("Creating board with %d strokes", strokesList.size());
        if (!whiteboardId.equals(listeningToBoard)) {
            throw new IllegalStateException("Not listening to board with id " + whiteboardId + " - use initAndStartRealtimeForBoard or do prepareRealtimeForBoard first");
        }

        List<Stroke> allStrokes = new ArrayList<>(bufferedStrokes.size() + strokesList.size());
        allStrokes.addAll(strokesList);
        allStrokes.addAll(bufferedStrokes);
        Whiteboard whiteboard = new Whiteboard(whiteboardId, allStrokes, backgroundContent, backgroundBitmap);
        cache.put(whiteboardId, whiteboard);
        this.bufferedStrokes.clear();
        this.isBufferingRealtimeStrokes = false;
        return whiteboard;
    }

    public synchronized void addStrokeToCurrentRealtimeBoard(Stroke stroke) {
        Whiteboard currentRealtimeBoard = getCurrentRealtimeBoard();
        if (isBufferingRealtimeStrokes) {
            bufferedStrokes.add(stroke);
        } else if (currentRealtimeBoard != null) {
            currentRealtimeBoard.addStroke(stroke);
        }
    }

    public synchronized void removeStrokeFromCurrentRealtimeBoard(UUID removeStrokeId) {
        Whiteboard currentRealtimeBoard = getCurrentRealtimeBoard();
        if (isBufferingRealtimeStrokes) {
            for (int i = 0; i < bufferedStrokes.size(); i++) {
                if (bufferedStrokes.get(i).hasSameId(removeStrokeId)) {
                    bufferedStrokes.remove(i);
                    break;
                }
            }
        } else if (currentRealtimeBoard != null) {
            currentRealtimeBoard.removeStroke(removeStrokeId);
        }
    }

    public synchronized void clearCurrentRealtimeBoard() {
        Whiteboard currentRealtimeBoard = getCurrentRealtimeBoard();
        if (currentRealtimeBoard != null) {
            currentRealtimeBoard.clear();
        }
    }

    public synchronized void markCurrentRealtimeBoardStale() {
        Ln.i("Stale current realtime board");
        Whiteboard currentRealtimeBoard = getCurrentRealtimeBoard();
        if (currentRealtimeBoard != null) {
            currentRealtimeBoard.setStale(true);
        }
    }

    public synchronized void evictAll() {
        cache.evictAll();
        listeningToBoard = null;
    }

    private synchronized Whiteboard getCurrentRealtimeBoard() {
        if (listeningToBoard != null) {
            return getWhiteboard(listeningToBoard);
        } else {
            return null;
        }
    }

    private synchronized void markAllBoardsAsStale() {
        Ln.v("[CacheTest] mark cache as stale");
        for (Whiteboard whiteboard : cache.snapshot().values()) {
            whiteboard.setStale(true);
        }
    }

    @SuppressWarnings("unused")
    public void onEventBackgroundThread(WhiteboardRealtimeEvent event) {
        if (listeningToBoard == null) {
            Ln.d("whiteboard cache is not listening to any board, will return");
            return;
        }

        if (!event.getBoardId().equals(listeningToBoard)) {
            Ln.d("the realtime message is not for the listening board, will return");
            return;
        }

        JsonObject realtimeMessage = event.getData();
        if (realtimeMessage != null) {
            JsonElement action = realtimeMessage.get(WhiteboardConstants.ACTION);
            String realtimeAction = action != null ? action.getAsString() : "No action specified";

            switch(realtimeAction) {
                case WhiteboardConstants.CONTENT_COMMIT:
                    addStrokeToCurrentRealtimeBoard(WhiteboardUtils.createStroke(realtimeMessage, gson));
                    break;

                case WhiteboardConstants.EVENT_END_CLEARBOARD:
                    clearCurrentRealtimeBoard();
            }
        }
    }

    @Override
    public void start() {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
        }
    }

    @Override
    public void stop() {
        eventBus.unregister(this);
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }
}
