package com.cisco.spark.android.whiteboard.realtime;

import com.cisco.spark.android.core.AuthenticatedUser;
import com.github.benoitdion.ln.Ln;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.ACTION;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.COLOR;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENTS_BUFFER;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_ARRAY;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_BEGIN;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_CANCEL;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_COMMIT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_TYPE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CONTENT_UPDATE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CURVE_ID;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CURVE_POINTS;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.CURVE_TYPE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.DISPLAY_NAME;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.DRAW_MODE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.EVENT_END_CLEARBOARD;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.EVENT_START_CLEARBOARD;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.ID;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.LAST_COMMIT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.NAME;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.R0;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.SENDER;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.STRIDE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.STYLE;

public class RealtimeFormatter {

    private static final String DEFAULT_REVISION = R0;

    public static JsonObject buildStartClearBoardJson() {
        return buildStartClearBoardJson(DEFAULT_REVISION);
    }

    private static JsonObject buildStartClearBoardJson(String revision) {

        JsonObject json = new JsonObject();
        switch (revision) {
            case R0:
            default:
                json.addProperty(ACTION, EVENT_START_CLEARBOARD);
                break;
        }

        return json;
    }

    public static JsonObject buildEndClearBoardJson(JsonArray allContentsToBeSend) {
        return buildEndClearBoardJson(DEFAULT_REVISION, allContentsToBeSend);
    }

    private static JsonObject buildEndClearBoardJson(String revision, JsonArray allContentsToBeSend) {

        JsonObject json = new JsonObject();
        switch (revision) {
            case R0:
            default:
                json.addProperty(ACTION, EVENT_END_CLEARBOARD);
                if (allContentsToBeSend != null) {
                    json.addProperty("error", "error");
                    json.add("contents", allContentsToBeSend);
                }
                break;
        }

        return json;
    }

    public static JsonObject buildContentBeginJson(String drawMode, JsonObject[] colorJsons, String[] writerIds, AuthenticatedUser authenticatedUser) {
        return buildContentBeginJson(DEFAULT_REVISION, drawMode, colorJsons, writerIds, authenticatedUser);
    }

    private static JsonObject buildContentBeginJson(String revision, String drawMode, JsonObject[] colorJsons, String[] writerIds, AuthenticatedUser authenticatedUser) {
        JsonObject json = new JsonObject();
        switch (revision) {
            case R0:
            default:
                JsonArray contentArray = new JsonArray();
                for (int i = 0; i < writerIds.length; i++) {
                    JsonObject contentStyle = new JsonObject();
                    try {
                        contentStyle.add(COLOR, colorJsons[i]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //default to the first color
                        contentStyle.add(COLOR, colorJsons[0]);
                    }
                    contentStyle.addProperty(DRAW_MODE, drawMode);
                    JsonObject curveMetaData = new JsonObject();
                    curveMetaData.add(STYLE, contentStyle);
                    curveMetaData.addProperty(CURVE_ID, writerIds[i]);
                    contentArray.add(curveMetaData);
                }
                json.add(CONTENT_ARRAY, contentArray);
                json.addProperty(CONTENT_TYPE, CURVE_TYPE);
                json.addProperty(NAME, CONTENT_BEGIN);
                json.addProperty(ACTION, CONTENT_BEGIN);

                if (authenticatedUser == null) {
                    Ln.e("Could not find the sender info.");
                    break;
                }

                JsonObject sender = new JsonObject();
                sender.addProperty(DISPLAY_NAME, authenticatedUser.getDisplayName());
                sender.addProperty(ID, authenticatedUser.getUserId());
                json.add(SENDER, sender);
                break;
        }

        return json;
    }

    public static JsonObject buildContentUpdateJson(JsonArray contentsBuffer, AuthenticatedUser authenticatedUser) {
        return buildContentUpdateJson(DEFAULT_REVISION, contentsBuffer, authenticatedUser);
    }

    private static JsonObject buildContentUpdateJson(String revision, JsonArray contentsBuffer, AuthenticatedUser authenticatedUser) {
        JsonObject json = new JsonObject();
        switch (revision) {
            case R0:
            default:
                json.add(CONTENTS_BUFFER, contentsBuffer);
                json.addProperty(ACTION, CONTENT_UPDATE);

                if (authenticatedUser == null) {
                    Ln.e("Could not find the sender info.");
                    break;
                }

                JsonObject sender = new JsonObject();
                sender.addProperty(DISPLAY_NAME, authenticatedUser.getDisplayName());
                sender.addProperty(ID, authenticatedUser.getUserId());
                json.add(SENDER, sender);
                break;
        }

        return json;
    }

    public static JsonObject buildContentCurveJson(JsonArray points, String writerId) {
        JsonObject content = new JsonObject();
        content.addProperty(CONTENT_TYPE, CURVE_TYPE);
        content.addProperty(NAME, CONTENT_UPDATE);

        JsonObject curvePoints = new JsonObject();
        curvePoints.addProperty(CURVE_ID, writerId);
        curvePoints.add(CURVE_POINTS, points);
        curvePoints.addProperty(STRIDE, 3);
        JsonArray contentArray = new JsonArray();
        contentArray.add(curvePoints);
        content.add(CONTENT_ARRAY, contentArray);
        return content;
    }

    public static JsonObject buildPersistedContentJson(JsonArray points, String writerId, String persistedId, String drawMode, JsonObject colorJson, boolean lastCommit, AuthenticatedUser authenticatedUser) {
        return buildPersistedContentJson(DEFAULT_REVISION, points, writerId, persistedId, drawMode, colorJson, lastCommit, authenticatedUser);
    }

    private static JsonObject buildPersistedContentJson(String revision, JsonArray points, String writerId, String persistedId, String drawMode, JsonObject colorJson, boolean lastCommit, AuthenticatedUser authenticatedUser) {
        JsonObject json = new JsonObject();
        switch (revision) {
            case R0:
            default:
                json.addProperty(CURVE_ID, writerId);
                json.addProperty(ID, persistedId);
                json.addProperty(CONTENT_TYPE, CURVE_TYPE);
                json.addProperty(NAME, CONTENT_COMMIT);
                json.addProperty(ACTION, CONTENT_COMMIT);
                json.addProperty(STRIDE, 3);
                json.addProperty(DRAW_MODE, drawMode);
                json.add(COLOR, colorJson);
                json.add(CURVE_POINTS, points);
                json.addProperty(LAST_COMMIT, lastCommit);

                if (authenticatedUser == null) {
                    Ln.e("Could not find the sender info.");
                    break;
                }
                JsonObject sender = new JsonObject();
                sender.addProperty(ID, authenticatedUser.getUserId());
                sender.addProperty(DISPLAY_NAME, authenticatedUser.getDisplayName());
                json.add(SENDER, sender);
                break;
        }
        return json;
    }

    public static JsonObject buildContentCancelJson(JsonArray points, String writerId, String drawMode, JsonObject colorJson, AuthenticatedUser authenticatedUser) {
        return buildContentCancelJson(DEFAULT_REVISION, points, writerId, drawMode, colorJson, authenticatedUser);
    }

    private static JsonObject buildContentCancelJson(String revision, JsonArray points, String writerId, String drawMode, JsonObject colorJson, AuthenticatedUser authenticatedUser) {
        JsonObject json = new JsonObject();
        switch (revision) {
            case R0:
            default:
                json.addProperty(CURVE_ID, writerId);
                json.addProperty(CONTENT_TYPE, CURVE_TYPE);
                json.addProperty(NAME, CONTENT_COMMIT);
                json.addProperty(ACTION, CONTENT_CANCEL);
                json.addProperty(STRIDE, 3);
                json.addProperty(DRAW_MODE, drawMode);
                json.add(COLOR, colorJson);
                json.add(CURVE_POINTS, points);

                if (authenticatedUser == null) {
                    Ln.e("Could not find the sender info.");
                    break;
                }
                JsonObject sender = new JsonObject();
                sender.addProperty(ID, authenticatedUser.getUserId());
                sender.addProperty(DISPLAY_NAME, authenticatedUser.getDisplayName());
                json.add(SENDER, sender);
                break;
        }
        return json;
    }
}
