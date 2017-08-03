package com.cisco.spark.android.model;

import android.net.*;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;

public class ActivityObjectTypeAdapter implements JsonDeserializer<ActivityObject>, JsonSerializer<ActivityObject> {
    private static Gson gson;

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .registerTypeHierarchyAdapter(Uri.class, new UriTypeAdapter())
                .create();
    }

    @Override
    public ActivityObject deserialize(JsonElement jsonElement, Type type,
                                      JsonDeserializationContext jsonDeserializationContext)
        throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();
        JsonElement objectTypeElement = object.get("objectType");
        String objectType = objectTypeElement != null ? objectTypeElement.getAsString() : "";

        if ("person".equals(objectType)) {
            return jsonDeserializationContext.<Person>deserialize(jsonElement, Person.class);
        } else if ("team".equals(objectType)) {
            return jsonDeserializationContext.<Team>deserialize(jsonElement, Team.class);
        } else if ("conversation".equals(objectType)) {
            return jsonDeserializationContext.<Conversation>deserialize(jsonElement, Conversation.class);
        } else if ("comment".equals(objectType)) {
            return jsonDeserializationContext.<Comment>deserialize(jsonElement, Comment.class);
        } else if ("activity".equals(objectType) || object.has("verb")) {
            return jsonDeserializationContext.<Activity>deserialize(jsonElement, Activity.class);
        } else if ("file".equals(objectType)) {
            return jsonDeserializationContext.<File>deserialize(jsonElement, File.class);
        } else if ("content".equals(objectType)) {
            return jsonDeserializationContext.<Content>deserialize(jsonElement, Content.class);
        } else if ("event".equals(objectType)) {
            return jsonDeserializationContext.<EventObject>deserialize(jsonElement, EventObject.class);
        } else if ("locusSessionSummary".equals(objectType)) {
            return jsonDeserializationContext.<EventObject>deserialize(jsonElement, LocusSessionSummary.class);
        } else {
            ActivityObject ret = new ActivityObject(objectType);
            ret.setId(getField(object, "id"));
            ret.setDisplayName(getField(object, "displayName"));
            JsonElement urlElement = object.get("url");
            if (urlElement != null) {
                ret.setUri(jsonDeserializationContext.<Uri>deserialize(urlElement, Uri.class));
            }
            JsonElement publishedElement = object.get("published");
            if (publishedElement != null) {
                ret.setPublished(jsonDeserializationContext.<Date>deserialize(publishedElement, Date.class));
            }
            return ret;
        }
    }

    public JsonElement serialize(ActivityObject id, Type typeOfId, JsonSerializationContext context) {
        if (id instanceof Content) {
            return context.serialize(id, Content.class);
        } else {
            return gson.toJsonTree(id);
        }
    }

    private String getField(JsonObject object, String name) {
        JsonPrimitive field = object.getAsJsonPrimitive(name);
        return field != null ? field.getAsString() : null;
    }
}
