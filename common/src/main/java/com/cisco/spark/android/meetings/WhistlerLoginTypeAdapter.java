package com.cisco.spark.android.meetings;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class WhistlerLoginTypeAdapter implements JsonDeserializer<WhistlerLoginType>, JsonSerializer<WhistlerLoginType> {

    @Override
    public WhistlerLoginType deserialize(JsonElement jsonElement, Type type,
                                        JsonDeserializationContext jsonDeserializationContext) {
        String loginTypeString = jsonElement.getAsString();
        WhistlerLoginType eventType = WhistlerLoginType.fromString(loginTypeString);
        return eventType;
    }

    @Override
    public JsonElement serialize(WhistlerLoginType src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
