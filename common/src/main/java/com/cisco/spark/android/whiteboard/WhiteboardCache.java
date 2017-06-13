package com.cisco.spark.android.whiteboard;

import android.util.LruCache;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardRealtimeEvent;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class WhiteboardCache implements Component {

    private static final int CACHE_SIZE_LIMIT = 5;

    private final EventBus eventBus;
    private final Gson gson;
    private final LruCache<String, Whiteboard> cache;
    private final JsonParser jsonParser;

    private String listeningToBoard;

    public WhiteboardCache(EventBus eventBus, Gson gson) {

        this.eventBus = eventBus;
        this.gson = gson;

        cache = new LruCache<>(CACHE_SIZE_LIMIT);
        jsonParser = new JsonParser();
    }

    public synchronized Whiteboard getWhiteboard(String boardId) {
        return cache.get(boardId);
    }

    public synchronized void initAndStartRealtimeForBoard(String cacheBoardId, List<Stroke> strokesList) {
        markAllBoardsAsStale();
        cache.put(cacheBoardId, new Whiteboard(cacheBoardId, new ArrayList<>(strokesList)));
        this.listeningToBoard = cacheBoardId;
    }

    public synchronized void addStrokeToCurrentRealtimeBoard(Stroke stroke) {
        Whiteboard currentRealtimeBoard = getCurrentRealtimeBoard();
        if (currentRealtimeBoard != null) {
            currentRealtimeBoard.addStroke(stroke);
        }
    }

    public synchronized void clearCurrentRealtimeBoard() {
        Whiteboard currentRealtimeBoard = getCurrentRealtimeBoard();
        if (currentRealtimeBoard != null) {
            currentRealtimeBoard.clear();
        }
    }

    public synchronized void evictAll() {
        cache.evictAll();
    }

    private synchronized Whiteboard getCurrentRealtimeBoard() {
        if (listeningToBoard != null) {
            return getWhiteboard(listeningToBoard);
        } else {
            return null;
        }
    }

    private synchronized void markAllBoardsAsStale() {
        for (Whiteboard whiteboard : cache.snapshot().values()) {
            whiteboard.setStale(true);
        }
    }

    @SuppressWarnings("unused")
    public void onEventBackgroundThread(WhiteboardRealtimeEvent event) {
        if (listeningToBoard != null) {
            JsonElement messageData = jsonParser.parse(event.getData());

            if (messageData != null) {
                JsonObject realtimeMessage = messageData.getAsJsonObject();
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
