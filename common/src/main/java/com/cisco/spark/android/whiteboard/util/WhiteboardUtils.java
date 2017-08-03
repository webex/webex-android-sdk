package com.cisco.spark.android.whiteboard.util;

import android.graphics.Color;
import android.view.MotionEvent;
import android.webkit.URLUtil;

import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.persistence.model.ContentItems;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.cisco.spark.android.whiteboard.renderer.LocalWILLWriter;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.wacom.ink.path.PathUtils;
import com.wacom.ink.rasterization.BlendMode;
import com.wacom.ink.rasterization.SolidColorBrush;
import com.wacom.ink.rasterization.StrokePaint;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import retrofit2.Response;

public class WhiteboardUtils {

    public static final String ROUTE_PREFIX = "board.";

    public static StrokePaint createStrokePaint(int color) {
        StrokePaint strokePaint = new StrokePaint();
        strokePaint.setStrokeBrush(new SolidColorBrush());
        strokePaint.setColor(color);
        strokePaint.setWidth(Float.NaN);
        return strokePaint;
    }

    public static StrokePaint getDefaultStrokePaint() {
        return createStrokePaint(Color.BLACK);
    }

    public static float[] scaleRawOutputPoints(float[] rawPoints, float scaleFactor) {
        //todo should we scale points as well in the JS ?

        float[] convertedPoints = new float[rawPoints.length];

        for (int i = 0; i < rawPoints.length; i++) {
            convertedPoints[i] = (float) (Math.floor(1000 * rawPoints[i] / scaleFactor)) / 1000;
        }
        return convertedPoints;
    }

    public static float[] scaleRawInputPoints(float[] rawPoints, float scaleFactor) {
        //TODO maybe not scale the width if eraser ? or at all ?

        float[] convertedPoints = new float[rawPoints.length];

        for (int i = 0; i < rawPoints.length; i++) {
            convertedPoints[i] = rawPoints[i] * scaleFactor;
        }
        return convertedPoints;
    }

    public static int convertColorJsonToInt(JsonObject colorJson) {
        //TODO change sending the color as argb int in javascript
        int colorInt = Color.BLACK;
        if (colorJson != null) {
            int alpha = colorJson.get("alpha").getAsInt();
            colorInt = Color.argb(alpha == 0 ? 0 : 255, colorJson.get("red").getAsInt(),
                                  colorJson.get("green").getAsInt(), colorJson.get("blue").getAsInt());
        }
        return colorInt;
    }

    public static JsonObject convertColorIntToJson(int colorInt) {
        //TODO change sending the color as argb int in javascript
        JsonObject colorJson = new JsonObject();
        colorJson.addProperty("red", Color.red(colorInt));
        colorJson.addProperty("green", Color.green(colorInt));
        colorJson.addProperty("blue", Color.blue(colorInt));
        colorJson.addProperty("alpha", 1);
        return colorJson;
    }

    /*
     * Workaround for bug in WILL, can switch to PhaseUtils.getPhaseFromMotionEvent if they fix it
     */
    public static PathUtils.Phase getPhaseFromMotionEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                return PathUtils.Phase.BEGIN;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                return PathUtils.Phase.END;
            case 2:
                return PathUtils.Phase.MOVE;
            default:
                return PathUtils.Phase.UNKNOWN;
        }
    }

    public static String buildRemoteWriterKey(UUID senderId, String curveId) {
        return senderId.toString() + WhiteboardConstants.REMOTE_WRITER_KEY_SEPARATOR + curveId;
    }

    public static String safeGetAsString(JsonElement jsonElement, String defaultValue) {
        if (jsonElement != null) {
            return jsonElement.getAsString();
        } else {
            return defaultValue;
        }
    }

    public static String getIdFromUrl(String url) {
        return url != null ? url.substring(url.lastIndexOf('/') + 1) : null;
    }

    public static String extractNextPageLinkFromResponse(Response response) {
        Headers header = response.headers();
        String link = "";
        if (header != null && header.get("Link") != null) {
            link = header.get("Link").replaceAll("[<>;]", "").split(" ")[0];
        }
        return link;
    }

    public static String sanitizeBinding(String channelID) {
        return ROUTE_PREFIX + channelID.replace('-', '.').replace('_', '#');
    }

    public static String unsanitizeBinding(String route) {
        return route.substring(ROUTE_PREFIX.length()).replace(".", "-").replace("#", "_");
    }

    public static String getNextUrl(Response<ContentItems> response) {
        String nextUrl = null;
        String link = response.headers().get("Link");
        if (link != null) {
            Pattern pattern = Pattern.compile("<([^>]+)>; rel=\"next\"");
            Matcher matcher = pattern.matcher(link);
            if (matcher.matches() && URLUtil.isNetworkUrl(matcher.group(1))) {
                nextUrl = matcher.group(1);
            }
        }
        return nextUrl;
    }

    public static Stroke createStroke(Content content, JsonParser jsonParser, Gson gson) {
        JsonElement parsedContent = null;
        try {
            parsedContent = jsonParser.parse(content.getPayload());
        } catch (JsonSyntaxException e) {
            Ln.e("Unable to parse json: " + e.getMessage());
        }

        if (parsedContent == null || !parsedContent.isJsonObject()) {
            Ln.e("Received content that is not a JSON object");
            return null;
        }

        JsonObject payload = parsedContent.getAsJsonObject();
        if (WhiteboardConstants.CURVE_TYPE
                .equalsIgnoreCase(payload.get(WhiteboardConstants.CONTENT_TYPE).getAsString())) {
            return createStroke(payload, gson);
        } else {
            return null;
        }
    }

    public static Stroke createStroke(JsonObject payload, Gson gson) {
        UUID strokeId = null;
        if (payload.has(WhiteboardConstants.CURVE_ID)) {
            strokeId = UUID.fromString(payload.get(WhiteboardConstants.CURVE_ID).getAsString());
        }

        float[] points = gson.fromJson(payload.getAsJsonArray("curvePoints"), float[].class);
        JsonElement drawMode = payload.get("drawMode");
        BlendMode blendMode;
        if (drawMode != null) {
            blendMode = "ERASE".equalsIgnoreCase(drawMode.getAsString()) ? BlendMode.BLENDMODE_ERASE : BlendMode.BLENDMODE_NORMAL;
        } else {
            blendMode = BlendMode.BLENDMODE_NORMAL;
        }
        Stroke stroke = new Stroke(
                strokeId,
                points,
                convertColorJsonToInt(payload.getAsJsonObject("color")),
                payload.get("stride").getAsInt(),
                blendMode
        );



        return stroke;
    }

    public static Stroke createStroke(LocalWILLWriter writer, float scaleFactor) {
        float[] points = scaleRawOutputPoints(writer.getPoints(), scaleFactor);
        return new Stroke(
                writer.getWriterId(),
                points,
                writer.getColor(),
                writer.getStride(),
                writer.getBlendMode()
        );
    }
}
