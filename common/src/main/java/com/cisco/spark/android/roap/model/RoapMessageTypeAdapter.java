package com.cisco.spark.android.roap.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class RoapMessageTypeAdapter implements JsonDeserializer<RoapBaseMessage>, JsonSerializer<RoapBaseMessage> {
    private static Gson gson;

    static {
        gson = new GsonBuilder().create();
    }

    @Override
    public RoapBaseMessage deserialize(JsonElement jsonElement, Type type,
                                      JsonDeserializationContext jsonDeserializationContext)
        throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();
        JsonElement objectTypeElement = object.get("messageType");
        String messageType = objectTypeElement != null ? objectTypeElement.getAsString() : "";

        if (RoapBaseMessage.OFFER.equals(messageType)) {
            return jsonDeserializationContext.<RoapOfferMessage>deserialize(jsonElement, RoapOfferMessage.class);
        } else if (RoapBaseMessage.ANSWER.equals(messageType)) {
            return jsonDeserializationContext.<RoapAnswerMessage>deserialize(jsonElement, RoapAnswerMessage.class);
        } else if (RoapBaseMessage.OK.equals(messageType)) {
            return jsonDeserializationContext.<RoapOkMessage>deserialize(jsonElement, RoapOkMessage.class);
        } else if (RoapBaseMessage.ERROR.equals(messageType)) {
            return jsonDeserializationContext.<RoapErrorMessage>deserialize(jsonElement, RoapErrorMessage.class);
        }

        return null;
    }

    public JsonElement serialize(RoapBaseMessage src, Type typeOfId, JsonSerializationContext context) {
        return gson.toJsonTree(src);
    }
}
