package com.cisco.spark.android.whiteboard;

import com.google.gson.JsonObject;

public class WhiteboardOriginalData {
    WhiteboardError.ErrorData errorData;
    JsonObject originalMessage;

    public WhiteboardError.ErrorData getErrorData() {
        return errorData;
    }

    public JsonObject getOriginalMessage() {
        return originalMessage;
    }

    public WhiteboardOriginalData(WhiteboardError.ErrorData errorData, JsonObject originalMessage) {
        this.errorData = errorData;
        this.originalMessage = originalMessage;
    }
}
