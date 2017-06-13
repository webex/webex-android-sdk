package com.cisco.spark.android.whiteboard.view.writer;

import android.graphics.Matrix;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.util.MatrixBuilder;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.WhiteboardController;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardMercuryUpdateEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardRealtimeEvent;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardServiceResponseEvent;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wacom.ink.rasterization.BlendMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.greenrobot.event.EventBus;

public class WhiteboardRealtimeWriter {

    private final WhiteboardController whiteboardController;
    private final WhiteboardService whiteboardService;
    private final ApiTokenProvider apiTokenProvider;
    private final JsonParser mJsonParser;
    private final EventBus bus;
    private final Gson gson;
    private final WhiteboardCache whiteboardCache;

    private JsonArray allContentsToBeSend;

    public WhiteboardRealtimeWriter(WhiteboardController whiteboardController, Gson gson, EventBus bus,
                                    ApiTokenProvider apiTokenProvider, WhiteboardService whiteboardService,
                                    WhiteboardCache whiteboardCache) {

        this.whiteboardController = whiteboardController;
        this.gson = gson;
        this.bus = bus;
        this.apiTokenProvider = apiTokenProvider;
        this.whiteboardService = whiteboardService;
        this.whiteboardCache = whiteboardCache;

        mJsonParser = new JsonParser();
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
    public JsonObject buildContentBeginJson(WhiteboardLocalWriter.Pointer pointer,
                                            WhiteboardLocalWriter.DrawMode drawMode) {

        LocalWILLWriter writer = pointer.writer;

        JsonObject contentStyle = new JsonObject();
        contentStyle.addProperty(WhiteboardConstants.DRAW_MODE, drawMode.toString());
        contentStyle.add(WhiteboardConstants.COLOR, WhiteboardUtils.convertColorIntToJson(writer.getColor()));
        JsonObject curveMetaData = new JsonObject();
        curveMetaData.add(WhiteboardConstants.STYLE, contentStyle);
        curveMetaData.addProperty(WhiteboardConstants.CURVE_ID, writer.getWriterId().toString());
        JsonArray contentArray = new JsonArray();
        contentArray.add(curveMetaData);

        JsonObject firstPoint = new JsonObject();
        firstPoint.addProperty(WhiteboardConstants.ID, 0);
        float[] point = new float[] { pointer.x, pointer.y };
        point = WhiteboardUtils.scaleRawOutputPoints(point, whiteboardController.getScaleFactor());
        firstPoint.addProperty("x", point[0]);
        firstPoint.addProperty("y", point[1]);
        JsonArray points = new JsonArray();
        points.add(firstPoint);

        JsonObject json = new JsonObject();
        json.add(WhiteboardConstants.CONTENT_ARRAY, contentArray);
        json.add(WhiteboardConstants.POINTS, points);
        json.addProperty(WhiteboardConstants.CONTENT_TYPE, WhiteboardConstants.CURVE_TYPE);
        json.addProperty(WhiteboardConstants.NAME, WhiteboardConstants.CONTENT_BEGIN);
        json.addProperty(WhiteboardConstants.ACTION, WhiteboardConstants.CONTENT_BEGIN);

        addSenderInfo(json);
        JsonObject overEncapsulatedMsg = new JsonObject();
        overEncapsulatedMsg.add("data", json);

        return overEncapsulatedMsg;
    }

    public JsonObject buildContentUpdateJson() {
        JsonObject json = new JsonObject();
        json.add(WhiteboardConstants.CONTENTS_BUFFER, buildContentsBuffer());
        json.addProperty(WhiteboardConstants.ACTION, WhiteboardConstants.CONTENT_UPDATE);

        addSenderInfo(json);
        JsonObject overEncapsulatedMsg = new JsonObject();
        overEncapsulatedMsg.add("data", json);

        return overEncapsulatedMsg;
    }

    private JsonArray buildContentsBuffer() {
        JsonArray contentsBuffer = new JsonArray();

        for (int pointerId = 0; pointerId < WhiteboardConstants.MAX_TOUCH_POINTERS; pointerId++) {
            LocalWILLWriter writer = whiteboardController.getLocalWriter(pointerId);
            if (writer != null) {
                for (float[] points : writer.getChangedPointsCurrentEvent()) {
                    JsonObject curvePoints = new JsonObject();
                    float[] scaledPoints = WhiteboardUtils
                                                   .scaleRawOutputPoints(points, whiteboardController.getScaleFactor());
                    curvePoints.add(WhiteboardConstants.CURVE_POINTS, gson.toJsonTree(scaledPoints));
                    curvePoints.addProperty(WhiteboardConstants.STRIDE, 3);
                    curvePoints.addProperty(WhiteboardConstants.CURVE_ID, writer.getWriterId().toString());
                    JsonArray contentArray = new JsonArray();
                    contentArray.add(curvePoints);
                    JsonObject content = new JsonObject();
                    content.add(WhiteboardConstants.CONTENT_ARRAY, contentArray);
                    content.addProperty(WhiteboardConstants.CONTENT_TYPE, WhiteboardConstants.CURVE_TYPE);
                    content.addProperty(WhiteboardConstants.NAME, WhiteboardConstants.CONTENT_UPDATE);
                    contentsBuffer.add(content);
                }
            }
        }

        return contentsBuffer;
    }

    private JsonObject buildContentCommitJson(UUID writerId, int indexStart, int indexEnd) {

        LocalWILLWriter writer = whiteboardController.getWritersBeingSaved().get(writerId);
        if (writer != null && !writer.isSaved()) {
            throw new IllegalStateException("Trying to send CONTENT_COMMIT for stroke which is not saved");
        }

        JsonObject json = whiteboardController.buildPersistedContentJson(writer, indexStart, indexEnd);

        addSenderInfo(json);

        JsonObject overEncapsulatedMsg = new JsonObject();
        overEncapsulatedMsg.add("data", json);
        return overEncapsulatedMsg;
    }

    private JsonObject buildContentCancelJson(UUID writerId) {

        LocalWILLWriter writer = whiteboardController.getWritersBeingSaved().get(writerId);

        if (writer != null && writer.isSaved()) {
            throw new IllegalStateException("Trying to send CONTENT_CANCEL for stroke which is actually saved");
        }

        JsonObject json = new JsonObject();
        json.addProperty(WhiteboardConstants.CURVE_ID, writer.getWriterId().toString());

        json.addProperty(WhiteboardConstants.CONTENT_TYPE, WhiteboardConstants.CURVE_TYPE);
        json.addProperty(WhiteboardConstants.NAME, WhiteboardConstants.CONTENT_CANCEL);
        json.addProperty(WhiteboardConstants.ACTION, WhiteboardConstants.CONTENT_CANCEL);

        addSenderInfo(json);

        JsonObject overEncapsulatedMsg = new JsonObject();
        overEncapsulatedMsg.add("data", json);
        return overEncapsulatedMsg;
    }

    public JsonObject buildStartClearBoardJson() {
        allContentsToBeSend = whiteboardController.allContents;

        JsonObject json = new JsonObject();
        json.addProperty(WhiteboardConstants.ACTION, WhiteboardConstants.EVENT_START_CLEARBOARD);

        addSenderInfo(json);

        JsonObject overEncapsulatedMsg = new JsonObject();
        overEncapsulatedMsg.add("data", json);
        return overEncapsulatedMsg;
    }

    public JsonObject buildEndClearBoardJson(boolean error) {
        JsonObject json = new JsonObject();
        json.addProperty(WhiteboardConstants.ACTION, WhiteboardConstants.EVENT_END_CLEARBOARD);
        if (error) {
            List<Content> allContentsList = new ArrayList<>();
            for (JsonElement payload : allContentsToBeSend) {
                allContentsList.add(whiteboardController.buildStrokeContent(payload.getAsJsonObject()));
            }
            whiteboardController.renderContents(allContentsList);

            json.addProperty("error", "error");
            json.add("contents", allContentsToBeSend);
        } else {
            whiteboardController.allContents = new JsonArray();
        }

        addSenderInfo(json);

        JsonObject overEncapsulatedMsg = new JsonObject();
        overEncapsulatedMsg.add("data", json);
        return overEncapsulatedMsg;
    }

    public void addSenderInfo(JsonObject json) {
        AuthenticatedUser authenticatedUser = apiTokenProvider.getAuthenticatedUserOrNull();
        if (authenticatedUser == null) {
            Ln.e("Could not find the sender info.");
            return;
        }
        JsonObject sender = new JsonObject();
        sender.addProperty(WhiteboardConstants.ID, authenticatedUser.getUserId());
        sender.addProperty(WhiteboardConstants.DISPLAY_NAME, authenticatedUser.getDisplayName());
        json.add(WhiteboardConstants.SENDER, sender);
    }

    /**
     * Receiving
     */

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(WhiteboardRealtimeEvent event) {

        if (!whiteboardController.isReady()) {
            // TODO: Bundle and replay these instead of ignoring
            Ln.w("The Whiteboard has not been initiated yet. Ignoring %s", event);
            // TODO: Metrics
            return;
        }

        JsonElement messageData = mJsonParser.parse(event.getData());
        if (messageData == null || !messageData.isJsonObject()) {
            Ln.e("Unable to render null or unsuitable");
            return;
        }

        JsonObject realtimeMessage = messageData.getAsJsonObject();

        JsonElement action = realtimeMessage.get(WhiteboardConstants.ACTION);
        String realtimeAction = action != null ? action.getAsString() : "No action specified";
        whiteboardController.abortSnapshot();

        switch (realtimeAction) {
            case WhiteboardConstants.CONTENT_BEGIN:
                startPartialContent(realtimeMessage);
                break;
            case WhiteboardConstants.CONTENT_UPDATE:
                updatePartialContent(realtimeMessage);
                break;
            case WhiteboardConstants.CONTENT_COMMIT:
                commitPartialContent(realtimeMessage);
                break;
            case WhiteboardConstants.CONTENT_CANCEL:
                cancelPartialContent(realtimeMessage);
                break;
            case WhiteboardConstants.EVENT_START_CLEARBOARD:
                bus.post(WhiteboardMercuryUpdateEvent.createClearAllEvent());
                whiteboardController.clearWhiteboardLocal();
                break;
            case WhiteboardConstants.EVENT_END_CLEARBOARD:
                if (realtimeMessage.has("error")) {
                    List<Content> allContentsList = new ArrayList<>();
                    for (JsonElement payload : realtimeMessage.get("contents").getAsJsonArray()) {
                        allContentsList.add(whiteboardController.buildStrokeContent(payload.getAsJsonObject()));
                    }

                    whiteboardController.renderContents(allContentsList);
                } else {
                    whiteboardController.clearWhiteboardLocal(); //if joined after the start event was sent
                }
                break;
            default:
                Ln.e(new IllegalArgumentException("Uncared for whiteboard RT message... :'( " + realtimeAction));
        }
    }

    private void startPartialContent(JsonObject realtimeMessage) {

        if (WhiteboardConstants.CURVE_TYPE.equalsIgnoreCase(
                safeGetAsString(realtimeMessage.get(WhiteboardConstants.CONTENT_TYPE), "NOCONTENTTYPE"))) {
            UUID senderId = getSenderUUID(realtimeMessage);
            if (senderId == null) {
                return;
            }

            JsonArray contentArray = realtimeMessage.getAsJsonArray(WhiteboardConstants.CONTENT_ARRAY);
            String curveId = safeGetAsString(contentArray.get(0).getAsJsonObject().get(WhiteboardConstants.CURVE_ID),
                                             "");

            String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);

            try {
                JsonObject style = contentArray.get(0).getAsJsonObject().get(WhiteboardConstants.STYLE)
                                               .getAsJsonObject();
                JsonObject color = style.get(WhiteboardConstants.COLOR).getAsJsonObject();
                int strokeColor = WhiteboardUtils.convertColorJsonToInt(color);
                String drawMode = style.get(WhiteboardConstants.DRAW_MODE).getAsString();
                BlendMode blendMode = WhiteboardConstants.ERASE.equalsIgnoreCase(
                        drawMode) ? BlendMode.BLENDMODE_ERASE : BlendMode.BLENDMODE_NORMAL;
                whiteboardController.createNewWILLWriter(remoteWriterKey, strokeColor, blendMode);
            } catch (NullPointerException e) {
                Ln.e("Malformatted json " + e);
            }
        }
    }

    private void updatePartialContent(JsonObject realtimeMessage) {
        if (WhiteboardConstants.CURVE_TYPE.equalsIgnoreCase(
                safeGetAsString(realtimeMessage.get(WhiteboardConstants.CONTENT_TYPE), "NOCONTENTTYPE"))) {
            UUID senderId = getSenderUUID(realtimeMessage);
            if (senderId == null) {
                return;
            }

            if (realtimeMessage.has(WhiteboardConstants.CONTENTS_BUFFER)) {
                for (JsonElement contentElement : realtimeMessage.getAsJsonArray(WhiteboardConstants.CONTENTS_BUFFER)) {
                    JsonObject content = contentElement.getAsJsonObject();
                    drawContent(content, senderId);
                }
            } else {
                drawContent(realtimeMessage, senderId);
            }

            Matrix matrix = new MatrixBuilder().setTranslateX(whiteboardController.getOffsetX())
                    .setTranslateY(whiteboardController.getOffsetY())
                    .setScaleFactor(whiteboardController.getRenderScaleFactor()).build();

            whiteboardController.renderViewWithMatrix(matrix);
        }
    }

    private void drawContent(JsonObject content, UUID senderId) {
        if (WhiteboardConstants.CURVE_TYPE
                    .equalsIgnoreCase(content.get(WhiteboardConstants.CONTENT_TYPE).getAsString())) {
            JsonArray contentArray = content.getAsJsonArray(WhiteboardConstants.CONTENT_ARRAY);
            if (contentArray.size() > 0) {
                JsonObject contentData = contentArray.get(0).getAsJsonObject();
                String curveId = safeGetAsString(contentData.get(WhiteboardConstants.CURVE_ID), "");
                String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);
                WILLWriter writer = whiteboardController.getRemoteWriter(remoteWriterKey);
                if (writer != null) {
                    writer.appendStrokeSegment(WhiteboardUtils.createStroke(contentData, gson));
                } else {
                    Ln.e("Trying to draw a curve with a disposed writer");
                    // TODO: Metrics
                }
            }
        }
    }

    private void commitPartialContent(JsonObject realtimeMessage) {

        if (WhiteboardConstants.CURVE_TYPE.equalsIgnoreCase(
                safeGetAsString(realtimeMessage.get(WhiteboardConstants.CONTENT_TYPE), "NOCONTENTTYPE"))) {
            UUID senderId = getSenderUUID(realtimeMessage);
            if (senderId == null) {
                return;
            }

            String curveId = safeGetAsString(realtimeMessage.get(WhiteboardConstants.CURVE_ID), "");
            String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);
            whiteboardController.removeRemoteWriter(remoteWriterKey);

            List<Content> contentsList = new ArrayList<>();
            contentsList.add(whiteboardController.buildStrokeContent(realtimeMessage));
            String boardId = whiteboardService.getBoardId();
            if (!contentsList.isEmpty()) {
                whiteboardController.commitRemoteWriter(remoteWriterKey, contentsList);
            }

            bus.post(new WhiteboardMercuryUpdateEvent(contentsList, boardId, false));
        }
    }

    private void cancelPartialContent(JsonObject realtimeMessage) {

        if (WhiteboardConstants.CURVE_TYPE.equalsIgnoreCase(
                safeGetAsString(realtimeMessage.get(WhiteboardConstants.CONTENT_TYPE), "NOCONTENTTYPE"))) {
            UUID senderId = getSenderUUID(realtimeMessage);
            if (senderId == null) {
                return;
            }

            String curveId = safeGetAsString(realtimeMessage.get(WhiteboardConstants.CURVE_ID), "");
            String remoteWriterKey = WhiteboardUtils.buildRemoteWriterKey(senderId, curveId);
            whiteboardController.cancelRemoteWriter(remoteWriterKey);
        }
    }

    private String safeGetAsString(JsonElement jsonElement, String defaultValue) {
        if (jsonElement != null) {
            return jsonElement.getAsString();
        } else {
            return defaultValue;
        }
    }

    private UUID getSenderUUID(JsonObject realtimeMessage) {
        UUID senderId;
        try {
            String senderUUID = realtimeMessage.get(WhiteboardConstants.SENDER)
                    .getAsJsonObject()
                    .get(WhiteboardConstants.ID)
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
                case WhiteboardConstants.SAVE_CONTENT:
                    saveContent(event);
                    break;
                case WhiteboardConstants.CLEAR_BOARD:
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
            if (!whiteboardController.getWritersBeingSaved().isEmpty()) {
                whiteboardService.realtimeMessage(buildContentCancelJson(writerId));
                whiteboardController.removeWriter(writerId);
            }
        } else {
            if (!whiteboardController.getWritersBeingSaved().isEmpty()) {
                LocalWILLWriter writer = whiteboardController.getWritersBeingSaved().get(writerId);
                if (writer != null) {
                    whiteboardCache.addStrokeToCurrentRealtimeBoard(WhiteboardUtils.createStroke(writer, whiteboardController.getScaleFactor()));
                    writer.setSaved(true);
                    whiteboardController.splitUpCurve(writer, (writer1, start, end) ->
                            whiteboardService.realtimeMessage(buildContentCommitJson(writerId, start, end)));
                }
            }
        }

        /*comment out the below line, do we really need to render view after saveContent?
        *
        * If we call renderView here, it will cause a bug:
        * If the user zoom in and drag the view to some location, then drawing some thing.
        * The surface will move to original location automatically.
        *
        * REASON: the offsetX and offsetY is not zero after drag, but renderView() here can't
        * get the scale and offset.
        */

        //whiteboardController.renderView();
    }
}
