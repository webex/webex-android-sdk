package com.cisco.spark.android.whiteboard.renderer;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.realtime.RealtimeFormatter;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardMercuryUpdateEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardRealtimeEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardServiceResponseEvent;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wacom.ink.rasterization.BlendMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;

import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.ACTION;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CLEAR_BOARD;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.COLOR;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENTS_BUFFER;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_ARRAY;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_BEGIN;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_CANCEL;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_COMMIT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_TYPE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_UPDATE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CURVE_ID;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CURVE_STALE_TIMEOUT_MS;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CURVE_TYPE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.DISPLAY_NAME;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.DRAW_MODE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.ERASE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.EVENT_END_CLEARBOARD;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.EVENT_START_CLEARBOARD;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.ID;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.LAST_COMMIT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.NORMAL;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SAVE_CONTENT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SENDER;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.STYLE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.maxTouchPointers;
import static com.cisco.spark.android.whiteboard.util.WhiteboardUtils.safeGetAsString;

public class WhiteboardRealtimeWriter {

    private final WhiteboardService whiteboardService;
    private final ApiTokenProvider apiTokenProvider;
    private final EventBus bus;
    private final Gson gson;
    private final WhiteboardCache whiteboardCache;
    private final WhiteboardRenderer whiteboardRenderer;

    private JsonArray allContentsToBeSend;

    private Map<String, List<Content>> remoteContentsToBeCommited;

    private Map<String, Long> activeContents;
    private Subscription timer;

    private SchedulerProvider schedulerProvider;

    private Clock clock;

    public WhiteboardRealtimeWriter(Gson gson, EventBus bus,
                                    ApiTokenProvider apiTokenProvider, WhiteboardService whiteboardService,
                                    WhiteboardCache whiteboardCache, Clock clock, SchedulerProvider schedulerProvider,
                                    WhiteboardRenderer whiteboardRenderer) {

        this.gson = gson;
        this.bus = bus;
        this.apiTokenProvider = apiTokenProvider;
        this.whiteboardService = whiteboardService;
        this.whiteboardCache = whiteboardCache;
        this.schedulerProvider = schedulerProvider;
        this.clock = clock;
        this.whiteboardRenderer = whiteboardRenderer;

        remoteContentsToBeCommited = new ConcurrentHashMap<>();

        activeContents = new ConcurrentHashMap<String, Long>();
        register();
    }

    public void unregister() {
        bus.unregister(this);
    }

    public void register() {
        if (!bus.isRegistered(this)) {
            bus.register(this);
        }
    }

    /**
     * Sending
     */
    public JsonObject buildContentBeginJson(List<WhiteboardLocalWriter.Pointer> pointers,
                                            WhiteboardLocalWriter.DrawMode drawMode) {
        JsonObject[] colorJsonsArray = new JsonObject[pointers.size()];
        String[] writerIds = new String[pointers.size()];
        for (int i = 0; i < pointers.size(); i++) {
            colorJsonsArray[i] = WhiteboardUtils.convertColorIntToJson(pointers.get(i).writer.getColor());
            writerIds[i] = pointers.get(i).writer.getWriterId().toString();
        }
        return RealtimeFormatter.buildContentBeginJson(
                drawMode.toString(),
                colorJsonsArray,
                writerIds,
                apiTokenProvider.getAuthenticatedUserOrNull()
                );
    }

    public JsonObject buildContentUpdateJson() {
        return RealtimeFormatter.buildContentUpdateJson(buildContentsBuffer(), apiTokenProvider.getAuthenticatedUserOrNull());
    }

    private JsonArray buildContentsBuffer() {
        JsonArray contentsBuffer = new JsonArray();

        for (int pointerId = 0; pointerId < maxTouchPointers; pointerId++) {
            LocalWILLWriter writer = whiteboardRenderer.getLocalWriter(pointerId);
            if (writer != null) {
                buildContentsBufferForWriter(writer, contentsBuffer);
            }
        }

        for (LocalWILLWriter writer: whiteboardRenderer.getWritersBeingSaved().values()) {
            buildContentsBufferForWriter(writer, contentsBuffer);
        }

        return contentsBuffer;
    }

    private void buildContentsBufferForWriter(LocalWILLWriter writer, JsonArray contentsBuffer) {
        for (float[] points : writer.getChangedPointsCurrentEvent()) {
            float[] scaledPoints = WhiteboardUtils
                    .scaleRawOutputPoints(points, whiteboardRenderer.getScaleFactor());
            contentsBuffer.add(
                    RealtimeFormatter.buildContentCurveJson(
                            gson.toJsonTree(scaledPoints).getAsJsonArray(),
                            writer.getWriterId().toString())
            );
        }
    }

    public JsonObject buildContentCommitJson(LocalWILLWriter writer, int indexStart, int indexEnd) {

        float[] points = Arrays.copyOfRange(writer.getPoints(), indexStart, indexEnd);
        float[] scaledPoints = WhiteboardUtils.scaleRawOutputPoints(points, whiteboardRenderer.getScaleFactor());

        return RealtimeFormatter.buildPersistedContentJson(
                gson.toJsonTree(scaledPoints).getAsJsonArray(),
                writer.getWriterId().toString(),
                "persisted id",
                writer.getBlendMode() == BlendMode.BLENDMODE_ERASE ? ERASE : NORMAL,
                WhiteboardUtils.convertColorIntToJson(writer.getColor()),
                writer.getPoints().length == indexEnd,
                apiTokenProvider.getAuthenticatedUserOrNull()
                );
    }

    private JsonObject buildContentCancelJson(UUID writerId) {

        LocalWILLWriter writer = whiteboardRenderer.getWritersBeingSaved().get(writerId);

        if (writer != null && writer.isSaved()) {
            throw new IllegalStateException("Trying to send CONTENT_CANCEL for stroke which is actually saved");
        }

        float[] scaledPoints = WhiteboardUtils.scaleRawOutputPoints(writer.getPoints(), whiteboardRenderer.getScaleFactor());

        return RealtimeFormatter.buildContentCancelJson(
                gson.toJsonTree(scaledPoints).getAsJsonArray(),
                writer.getWriterId().toString(),
                writer.getBlendMode() == BlendMode.BLENDMODE_ERASE ? ERASE : NORMAL,
                WhiteboardUtils.convertColorIntToJson(writer.getColor()),
                apiTokenProvider.getAuthenticatedUserOrNull()
        );
    }

    public JsonObject buildStartClearBoardJson() {
        allContentsToBeSend = whiteboardRenderer.allContents;

        JsonObject json = RealtimeFormatter.buildStartClearBoardJson();

        addSenderInfo(json);
        return json;
    }

    public JsonObject buildEndClearBoardJson(boolean error) {
        JsonObject json;
        if (error) {
            List<Content> allContentsList = new ArrayList<>();
            for (JsonElement payload : allContentsToBeSend) {
                allContentsList.add(whiteboardRenderer.buildStrokeContent(payload.getAsJsonObject()));
            }
            whiteboardRenderer.renderContents(allContentsList, null);

            json = RealtimeFormatter.buildEndClearBoardJson(allContentsToBeSend);
        } else {
            whiteboardRenderer.allContents = new JsonArray();
            json = RealtimeFormatter.buildEndClearBoardJson(null);
        }

        addSenderInfo(json);
        return json;
    }

    public void addSenderInfo(JsonObject json) {
        AuthenticatedUser authenticatedUser = apiTokenProvider.getAuthenticatedUserOrNull();
        if (authenticatedUser == null) {
            Ln.e("Could not find the sender info.");
            return;
        }
        JsonObject sender = new JsonObject();
        sender.addProperty(ID, authenticatedUser.getUserId());
        sender.addProperty(DISPLAY_NAME, authenticatedUser.getDisplayName());
        json.add(SENDER, sender);
    }

    /**
     * Receiving
     */

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(WhiteboardRealtimeEvent event) {
        if (!whiteboardRenderer.isReady()) {
            // TODO: Bundle and replay these instead of ignoring
            Ln.w("The Whiteboard has not been initiated yet. Ignoring %s", event);
            // TODO: Metrics
            return;
        }

        if (!event.getBoardId().equals(whiteboardService.getBoardId())) {
            Ln.w("The whiteboard realtime message is not for the current board, will discard it");
            return;
        }

        whiteboardRenderer.restoreCanvasIfNeeded();

        JsonElement messageData = event.getData();
        if (messageData == null || !messageData.isJsonObject()) {
            Ln.e("Unable to render null or unsuitable");
            return;
        }

        JsonObject realtimeMessage = messageData.getAsJsonObject();

        JsonElement action = realtimeMessage.get(ACTION);
        String realtimeAction = action != null ? action.getAsString() : "No action specified";
        whiteboardRenderer.abortSnapshot();

        switch (realtimeAction) {
            case CONTENT_BEGIN:
                startPartialContent(realtimeMessage);
                break;
            case CONTENT_UPDATE:
                updatePartialContent(realtimeMessage);
                break;
            case CONTENT_COMMIT:
                commitPartialContent(realtimeMessage);
                break;
            case CONTENT_CANCEL:
                cancelPartialContent(realtimeMessage);
                break;
            case EVENT_START_CLEARBOARD:
                bus.post(WhiteboardMercuryUpdateEvent.createClearAllEvent());
                whiteboardRenderer.clearWhiteboardLocal();
                break;
            case EVENT_END_CLEARBOARD:
                if (realtimeMessage.has("error")) {
                    List<Content> allContentsList = new ArrayList<>();
                    for (JsonElement payload : realtimeMessage.get("contents").getAsJsonArray()) {
                        allContentsList.add(whiteboardRenderer.buildStrokeContent(payload.getAsJsonObject()));
                    }

                    whiteboardRenderer.renderContents(allContentsList, null);
                } else {
                    whiteboardRenderer.clearWhiteboardLocal(); //if joined after the start event was sent
                }
                break;
            default:
                Ln.e(new IllegalArgumentException("Uncared for whiteboard RT message... :'( " + realtimeAction));
        }
    }

    private void startPartialContent(JsonObject realtimeMessage) {

        if (CURVE_TYPE.equalsIgnoreCase(
                safeGetAsString(realtimeMessage.get(CONTENT_TYPE), "NOCONTENTTYPE"))) {
            UUID senderId = getSenderUUID(realtimeMessage);
            if (senderId == null) {
                return;
            }

            JsonArray contentArray = realtimeMessage.getAsJsonArray(CONTENT_ARRAY);
            for (JsonElement content : contentArray) {
                JsonObject contentObject = content.getAsJsonObject();
                String curveId = safeGetAsString(contentObject.get(CURVE_ID), "");

                String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);

                activeContents.put(remoteWriterKey, clock.monotonicNow());

                if (timer == null || timer.isUnsubscribed()) {
                    timer = Observable.interval(5, TimeUnit.SECONDS, schedulerProvider.computation())
                            .observeOn(schedulerProvider.mainThread())
                            .subscribe(
                                    aLong -> {
                                        for (Map.Entry<String, Long> activeContent: activeContents.entrySet()) {
                                            if (clock.monotonicNow() > activeContent.getValue() + CURVE_STALE_TIMEOUT_MS) {
                                                Ln.e(new TimeoutException(), "Cancelling curve because stale. " + activeContent.getKey());
                                                whiteboardRenderer.cancelRemoteWriter(activeContent.getKey());
                                                activeContents.remove(activeContent.getKey());
                                                if (activeContents.isEmpty() && timer != null) {
                                                    timer.unsubscribe();
                                                }
                                            }
                                        }
                                    },
                                    Ln::e);
                }

                try {
                    JsonObject style = contentObject.get(STYLE)
                            .getAsJsonObject();
                    JsonObject color = style.get(COLOR).getAsJsonObject();
                    int strokeColor = WhiteboardUtils.convertColorJsonToInt(color);
                    String drawMode = style.get(DRAW_MODE).getAsString();
                    BlendMode blendMode = ERASE.equalsIgnoreCase(
                            drawMode) ? BlendMode.BLENDMODE_ERASE : BlendMode.BLENDMODE_NORMAL;
                    whiteboardRenderer.createNewWILLWriter(remoteWriterKey, strokeColor, blendMode);
                } catch (NullPointerException e) {
                    Ln.e("Malformatted json " + e);
                }
            }
            }
    }

    private void updatePartialContent(JsonObject realtimeMessage) {
        UUID senderId = getSenderUUID(realtimeMessage);
        if (senderId == null) {
            return;
        }

        if (realtimeMessage.has(CONTENTS_BUFFER)) {
            for (JsonElement contentElement : realtimeMessage.getAsJsonArray(CONTENTS_BUFFER)) {
                JsonObject content = contentElement.getAsJsonObject();
                drawContent(content, senderId);
            }
        } else {
            drawContent(realtimeMessage, senderId);
        }

        whiteboardRenderer.renderView();
    }

    private void drawContent(JsonObject content, UUID senderId) {
        if (CURVE_TYPE
                    .equalsIgnoreCase(content.get(CONTENT_TYPE).getAsString())) {
            JsonArray contentArray = content.getAsJsonArray(CONTENT_ARRAY);
            if (contentArray.size() > 0) {
                JsonObject contentData = contentArray.get(0).getAsJsonObject();
                String curveId = safeGetAsString(contentData.get(CURVE_ID), "");
                String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);
                WILLWriter writer = whiteboardRenderer.getRemoteWriter(remoteWriterKey);
                if (writer != null) {
                    writer.appendStrokeSegment(WhiteboardUtils.createStroke(contentData, gson));
                } else {
                    Ln.e("Trying to draw a curve with a disposed writer");
                    // TODO: Metrics
                }
                activeContents.put(remoteWriterKey, clock.monotonicNow());
            }
        }
    }

    private void commitPartialContent(JsonObject realtimeMessage) {

        if (CURVE_TYPE.equalsIgnoreCase(
                safeGetAsString(realtimeMessage.get(CONTENT_TYPE), "NOCONTENTTYPE"))) {
            UUID senderId = getSenderUUID(realtimeMessage);
            if (senderId == null) {
                return;
            }

            String curveId = safeGetAsString(realtimeMessage.get(CURVE_ID), "");
            String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);
            whiteboardRenderer.removeRemoteWriter(remoteWriterKey);

            boolean isCurveComplete = realtimeMessage.get(LAST_COMMIT) == null || realtimeMessage.get(LAST_COMMIT).getAsBoolean();
            List<Content> contentsList = remoteContentsToBeCommited.get(remoteWriterKey);
            if (contentsList == null) {
                contentsList = new ArrayList<>();
                remoteContentsToBeCommited.put(remoteWriterKey, contentsList);
            }
            contentsList.add(whiteboardRenderer.buildStrokeContent(realtimeMessage));
            String boardId = whiteboardService.getBoardId();
            if (!contentsList.isEmpty() && isCurveComplete) {
                whiteboardRenderer.commitRemoteWriter(remoteWriterKey, contentsList);
                remoteContentsToBeCommited.remove(remoteWriterKey);
                activeContents.remove(remoteWriterKey);
                if (activeContents.isEmpty() && timer != null) {
                    timer.unsubscribe();
                }
            }

            bus.post(new WhiteboardMercuryUpdateEvent(contentsList, boardId, false));
        }
    }

    private void cancelPartialContent(JsonObject realtimeMessage) {

        if (CURVE_TYPE.equalsIgnoreCase(
                safeGetAsString(realtimeMessage.get(CONTENT_TYPE), "NOCONTENTTYPE"))) {
            UUID senderId = getSenderUUID(realtimeMessage);
            if (senderId == null) {
                return;
            }

            String curveId = safeGetAsString(realtimeMessage.get(CURVE_ID), "");
            String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);
            whiteboardRenderer.cancelRemoteWriter(remoteWriterKey);
            activeContents.remove(remoteWriterKey);
        }
    }


    private UUID getSenderUUID(JsonObject realtimeMessage) {
        UUID senderId;
        try {
            String senderUUID = realtimeMessage.get(SENDER)
                    .getAsJsonObject()
                    .get(ID)
                    .getAsString();

            senderId = UUID.fromString(senderUUID);
        } catch (NullPointerException | IllegalArgumentException e) {
            Ln.e(e, "Malformatted whiteboard realtime message");
            return null;
        }
        return senderId;
    }

    public void onEvent(WhiteboardServiceResponseEvent event) {
        if (event.getAction() != null && !event.getAction().isEmpty()) {
            switch (event.getAction()) {
                case SAVE_CONTENT:
                    saveContent(event);
                    break;
                case CLEAR_BOARD:
                    if (event.getError() == null) {
                        whiteboardCache.clearCurrentRealtimeBoard();
                    }
                    whiteboardService.realtimeMessage(buildEndClearBoardJson(event.getError() != null));
                    break;
                default:
                    Ln.e(new IllegalArgumentException("Unknown action returned from the whiteboard service " +
                                                      event.getAction()));
            }
        } else {
            Ln.e(new NullPointerException("No action was returned from the whiteboard service"));
        }
    }

    private void saveContent(WhiteboardServiceResponseEvent event) {

        JsonObject response = event.getResponse();
        if (response == null) {
            Ln.e("[WhiteboardServiceEvent] Received event with null response");
            return;
        }

        JsonElement id = response.get("writerId");
        if (id == null) {
            Ln.e("[WhiteboardServiceEvent] Received response with no writerId");
            return;
        }

        UUID writerId;

        try {
            writerId = UUID.fromString(id.getAsString());
        } catch (Exception e) {
            Ln.e(e);
            return;
        }

        if (event.getError() != null) {
            Ln.e(event.getError().toString());
            if (!whiteboardRenderer.getWritersBeingSaved().isEmpty()) {
                whiteboardService.realtimeMessage(buildContentCancelJson(writerId));
                whiteboardRenderer.removeWriter(writerId);
            }
        } else {
            if (!whiteboardRenderer.getWritersBeingSaved().isEmpty()) {
                LocalWILLWriter writer = whiteboardRenderer.getWritersBeingSaved().get(writerId);
                if (writer != null) {
                    writer.setSaved(true);
                    whiteboardRenderer.splitUpCurve(writer, (sameWriter, start, end) ->
                            whiteboardService.realtimeMessage(buildContentCommitJson(sameWriter, start, end)));
                }
            }
        }

    }
}
