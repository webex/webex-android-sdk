package com.cisco.spark.android.locus.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class LocusKeyTypeAdapter implements JsonDeserializer<LocusKey>, JsonSerializer<LocusKey> {
    @Override
    public LocusKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String uriString = json.getAsString();
        return LocusKey.fromString(uriString);
    }

    @Override
    public JsonElement serialize(LocusKey src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null)
            return null;
        return new JsonPrimitive(src.getUrl().toString());
    }
}
