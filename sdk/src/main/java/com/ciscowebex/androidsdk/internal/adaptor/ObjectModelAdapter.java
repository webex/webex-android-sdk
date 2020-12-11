/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.adaptor;

import android.net.Uri;
import com.ciscowebex.androidsdk.internal.model.*;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;

public class ObjectModelAdapter implements JsonDeserializer<ObjectModel>, JsonSerializer<ObjectModel> {

    private static Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new SimpleAdaptor.DateType())
            .registerTypeHierarchyAdapter(Uri.class, new SimpleAdaptor.UriType())
            .create();

    @Override
    public ObjectModel deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();
        JsonElement objectTypeElement = object.get("objectType");
        String objectType = objectTypeElement != null ? objectTypeElement.getAsString() : "";

        if (ObjectModel.Type.person.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, PersonModel.class);
        }
        else if (ObjectModel.Type.team.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, TeamModel.class);
        }
        else if (ObjectModel.Type.conversation.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, ConversationModel.class);
        }
        else if (ObjectModel.Type.comment.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, CommentModel.class);
        }
        else if (ObjectModel.Type.activity.equals(objectType) || object.has("verb")) {
            return jsonDeserializationContext.deserialize(jsonElement, ActivityModel.class);
        }
        else if (ObjectModel.Type.file.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, FileModel.class);
        }
        else if (ObjectModel.Type.content.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, ContentModel.class);
        }
        else if (ObjectModel.Type.spaceProperty.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, SpacePropertyModel.class);
        }
        else if (ObjectModel.Type.groupMention.equals(objectType)) {
            return jsonDeserializationContext.deserialize(jsonElement, GroupMentionModel.class);
        }
        else {
            ObjectModel ret = new ObjectModel(objectType);
            ret.setId(getField(object, "id"));
            ret.setDisplayName(getField(object, "displayName"));
            JsonElement urlElement = object.get("url");
            if (urlElement != null) {
                ret.setUrl(jsonDeserializationContext.deserialize(urlElement, Uri.class));
            }
            JsonElement publishedElement = object.get("published");
            if (publishedElement != null) {
                ret.setPublished(jsonDeserializationContext.deserialize(publishedElement, Date.class));
            }
            return ret;
        }
    }

    public JsonElement serialize(ObjectModel model, Type typeOfId, JsonSerializationContext context) {
        if (model instanceof ContentModel) {
            return context.serialize(model, ContentModel.class);
        } else {
            return gson.toJsonTree(model);
        }
    }

    private String getField(JsonObject object, String name) {
        JsonPrimitive field = object.getAsJsonPrimitive(name);
        return field != null ? field.getAsString() : null;
    }

}
