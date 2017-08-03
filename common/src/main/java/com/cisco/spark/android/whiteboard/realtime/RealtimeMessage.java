package com.cisco.spark.android.whiteboard.realtime;

import android.net.Uri;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.UUID;

/**
 *
 */
public class RealtimeMessage {

    private final String id;
    private final String type;
    private final JsonArray recipients;
    private final JsonObject data;

    public RealtimeMessage(String channelId, String encryptedBlob, Uri keyUrl) {
        this.id = UUID.randomUUID().toString();
        this.type = "publishRequest";

        JsonObject recipient = new JsonObject();
        recipient.addProperty("alertType", "none");
        recipient.addProperty("route", "board." + channelId.replace('-', '.').replace('_', '#'));
        recipient.add("headers", new JsonObject());
        this.recipients = new JsonArray();
        this.recipients.add(recipient);


        JsonObject envelope = new JsonObject();
        envelope.addProperty("encryptionKeyUrl", keyUrl.toString());
        envelope.addProperty("channelId", channelId);

        JsonObject data = new JsonObject();
        data.addProperty("eventType", "board.activity");
        data.add("envelope", envelope);
        data.addProperty("payload", encryptedBlob);
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public JsonObject getData() {
        return data;
    }

    public String getType() {
        return this.type;
    }

    public JsonArray getRecipients() {
        return this.recipients;
    }

}
