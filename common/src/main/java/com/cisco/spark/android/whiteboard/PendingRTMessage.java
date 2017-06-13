package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.google.gson.JsonObject;

class PendingRTMessage {
    private JsonObject message;
    private Channel channel;

    public PendingRTMessage(JsonObject message, Channel channel) {
        this.message = message;
        this.channel = channel;
    }

    public JsonObject getMessage() {
        return message;
    }

    public Channel getChannel() {
        return channel;
    }
}
