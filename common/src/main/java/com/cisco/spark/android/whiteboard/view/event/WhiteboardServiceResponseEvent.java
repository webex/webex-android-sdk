package com.cisco.spark.android.whiteboard.view.event;

import android.support.annotation.Nullable;

import com.google.gson.JsonObject;

public class WhiteboardServiceResponseEvent {

    @Nullable private String action;
    @Nullable private JsonObject response;
    @Nullable private JsonObject error;

    public WhiteboardServiceResponseEvent(String action, JsonObject response, JsonObject error) {
        this.action = action;
        this.response = response;
        this.error = error;
    }

    @Nullable
    public String getAction() {
        return action;
    }

    @Nullable
    public JsonObject getResponse() {
        return response;
    }

    @Nullable
    public JsonObject getError() {
        return error;
    }
}
