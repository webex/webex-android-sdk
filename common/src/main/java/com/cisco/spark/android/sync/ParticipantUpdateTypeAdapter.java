package com.cisco.spark.android.sync;

import com.google.gson.*;

import java.lang.reflect.*;

/**
 * Type adapter to manage the {@link ConversationContract.ActivityEntry.Type#LEFT_CONVERSATION} payload format transition
 * <p/>
 * Old format: A single actor key
 * New format: An actor and an object. This actor is necessary to support both self leave and participant getting kicked out.
 */
public class ParticipantUpdateTypeAdapter implements JsonDeserializer<ParticipantUpdate> {
    @Override
    public ParticipantUpdate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        try {
            JsonObject object = json.getAsJsonObject();
            JsonElement actorElement = object.get("actor");
            JsonElement objectElement = object.get("object");
            return new ParticipantUpdate(context.<ActorRecord.ActorKey>deserialize(actorElement, ActorRecord.ActorKey.class), context.<ActorRecord.ActorKey>deserialize(objectElement, ActorRecord.ActorKey.class), null);
        } catch (IllegalStateException e) {
            ActorRecord.ActorKey key = new ActorRecord.ActorKey(json.getAsString());
            return new ParticipantUpdate(key, key, null);
        }
    }
}
