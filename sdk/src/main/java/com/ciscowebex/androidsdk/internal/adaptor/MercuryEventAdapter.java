/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import com.ciscowebex.androidsdk.internal.mercury.*;
import com.ciscowebex.androidsdk.utils.Json;
import com.github.benoitdion.ln.Ln;
import com.google.gson.*;
import me.helloworld.utils.Checker;

import java.lang.reflect.Type;

public class MercuryEventAdapter implements JsonDeserializer<MercuryEvent>  {

    public static class MercuryEventTypeAdapter implements JsonDeserializer<MercuryEvent.Type>, JsonSerializer<MercuryEvent.Type> {

        @Override
        public MercuryEvent.Type deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            String eventTypeString = jsonElement.getAsString();
            return MercuryEvent.Type.fromString(eventTypeString);
        }

        @Override
        public JsonElement serialize(MercuryEvent.Type src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    @Override
    public MercuryEvent deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
        JsonObject object = jsonElement.getAsJsonObject();
        JsonElement eventTypeElement = object.get("eventType");
        if (eventTypeElement == null) {
            return null;
        }
        String eventType = eventTypeElement.getAsString();
        if (Checker.isEmpty(eventType)) {
            Ln.d("No eventType in Mercury payload.");
            return null;
        }
        Ln.d("Mercury event type: %s", eventType);
        MercuryEvent event = null;
        if (eventType.equals(MercuryEvent.Type.CONVERSATION_ACTIVITY.phrase())) {
            event = jsonDeserializationContext.deserialize(jsonElement, MercuryActivityEvent.class);
        }
        else if (eventType.equals(MercuryEvent.Type.KMS_MESSAGE.phrase())) {
            event = jsonDeserializationContext.deserialize(jsonElement, MercuryKmsMessageEvent.class);
        }
        else if (eventType.equals(MercuryEvent.Type.KEY_PUSH.phrase())) {
            jsonElement = Json.extractJsonObjectFromString(jsonElement);
            event = jsonDeserializationContext.deserialize(jsonElement, MercuryKmsPushEvent.class);
        }
        else if (eventType.equals(MercuryEvent.Type.KMS_ACK.phrase())) {
            event = jsonDeserializationContext.deserialize(jsonElement, MercuryKmsAckEvent.class);
        }
        else if (eventType.startsWith("locus")) {
            event = jsonDeserializationContext.deserialize(jsonElement, MercuryLocusEvent.class);
        }
        else if (eventType.equals(MercuryEvent.Type.START_TYPING.phrase()) || eventType.equals(MercuryEvent.Type.STOP_TYPING.phrase())) {
            event = jsonDeserializationContext.deserialize(jsonElement, MercuryTypingEvent.class);
        }
        else {
            Ln.d("Unsupport mercury event: %s", eventType);
        }
        return event;
    }
}
