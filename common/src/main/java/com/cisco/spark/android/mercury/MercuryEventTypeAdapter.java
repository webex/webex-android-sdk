package com.cisco.spark.android.mercury;

import com.google.gson.*;

import java.lang.reflect.Type;

public class
        MercuryEventTypeAdapter implements JsonDeserializer<MercuryEventType>, JsonSerializer<MercuryEventType> {
    @Override
    public MercuryEventType deserialize(JsonElement jsonElement, Type type,
                                      JsonDeserializationContext jsonDeserializationContext) {
        String eventTypeString = jsonElement.getAsString();
        MercuryEventType eventType = MercuryEventType.fromString(eventTypeString);
        return eventType;
    }

    @Override
    public JsonElement serialize(MercuryEventType src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
