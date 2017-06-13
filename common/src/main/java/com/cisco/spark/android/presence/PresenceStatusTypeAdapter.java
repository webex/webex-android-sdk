package com.cisco.spark.android.presence;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class PresenceStatusTypeAdapter implements JsonDeserializer<PresenceStatus>, JsonSerializer<PresenceStatus> {

    @Override
    public PresenceStatus deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String statusTypeString = jsonElement.getAsString();
        PresenceStatus statusType = PresenceStatus.fromString(statusTypeString);
        return statusType;
    }

    @Override
    public JsonElement serialize(PresenceStatus src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
