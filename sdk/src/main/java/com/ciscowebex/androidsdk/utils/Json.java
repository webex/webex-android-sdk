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

package com.ciscowebex.androidsdk.utils;

import android.net.Uri;
import com.cisco.wx2.diagnostic_events.EventType;
import com.cisco.wx2.diagnostic_events.EventTypeDeserializer;
import com.ciscowebex.androidsdk.internal.adaptor.MercuryEventAdapter;
import com.ciscowebex.androidsdk.internal.adaptor.ObjectModelAdapter;
import com.ciscowebex.androidsdk.internal.adaptor.SimpleAdaptor;
import com.ciscowebex.androidsdk.internal.mercury.MercuryEvent;
import com.ciscowebex.androidsdk.internal.model.LocusKeyModel;
import com.ciscowebex.androidsdk.internal.model.ObjectModel;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.joda.time.Instant;

import java.lang.reflect.Type;
import java.util.Date;

public class Json {

    private static Gson gson = buildGson();

    public static Gson get() {
        return gson;
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return get().fromJson(json, classOfT);
        }
        catch (Throwable t) {
            Ln.e("Parse json error: " + t);
            return null;
        }
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        try {
            return get().fromJson(json, typeOfT);
        }
        catch (Throwable t) {
            Ln.e("Parse json error: " + t);
            return null;
        }
    }

    public static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new SimpleAdaptor.DateType())
                .registerTypeAdapter(Instant.class, new SimpleAdaptor.InstantType())
                .registerTypeHierarchyAdapter(Uri.class, new SimpleAdaptor.UriType())
                .registerTypeHierarchyAdapter(LocusKeyModel.class, new SimpleAdaptor.LocusKeyType())
                .registerTypeAdapter(MercuryEvent.class, new MercuryEventAdapter())
                .registerTypeAdapter(MercuryEvent.Type.class, new MercuryEventAdapter.MercuryEventTypeAdapter())
                .registerTypeAdapter(ObjectModel.class, new ObjectModelAdapter())
                .registerTypeAdapter(EventType.class, new EventTypeDeserializer())
                .create();
    }

    public static JsonElement extractJsonObjectFromString(JsonElement jsonElement) {
        String replaceString = jsonElement.toString().replace("\\", "");
        replaceString = replaceString.replaceAll("\"\\[", "[").replaceAll("\\]\"", "]");
        jsonElement = new JsonParser().parse(replaceString);
        return jsonElement;
    }

    public static String stringify(String userIdString) {
        if (userIdString == null) {
            return null;
        }
        userIdString = userIdString.replaceAll("\\[", "\\[\"");
        userIdString = userIdString.replaceAll("\\]", "\"]");
        userIdString = userIdString.replaceAll(",", "\",\"");
        userIdString = userIdString.replaceAll("\\s", "");
        return userIdString;
    }
}
