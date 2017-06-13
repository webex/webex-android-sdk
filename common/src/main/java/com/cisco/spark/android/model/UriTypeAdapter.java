package com.cisco.spark.android.model;

import android.net.*;
import com.google.gson.*;

import java.lang.reflect.*;

public class UriTypeAdapter implements JsonDeserializer<Uri>, JsonSerializer<Uri> {
    @Override
    public Uri deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String uriString = json.getAsString();
        try {
            return Uri.parse(uriString);
        } catch (Exception e) {
            throw new JsonSyntaxException(uriString, e);
        }
    }

    @Override
    public JsonElement serialize(Uri src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null)
            return null;
        return new JsonPrimitive(src.toString());
    }
}
