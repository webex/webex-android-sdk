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
import com.ciscowebex.androidsdk.internal.model.LocusKeyModel;
import com.ciscowebex.androidsdk.utils.DateUtils;
import com.google.gson.*;
import org.joda.time.Instant;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

public class SimpleAdaptor {

    public static class DateType implements JsonSerializer<Date> {

        private ThreadLocal<DateFormat> threadLocalDateFormat = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return DateUtils.buildIso8601Format();
            }
        };

        @Override
        public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
            return date == null ? null : new JsonPrimitive(threadLocalDateFormat.get().format(date));
        }
    }

    public static class InstantType implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json.getAsString() != null && !json.getAsString().isEmpty() ? Instant.parse(json.getAsString()) : null;
        }
    }

    public static class UriType implements JsonDeserializer<Uri>, JsonSerializer<Uri> {
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
            return src == null ? null :  new JsonPrimitive(src.toString());
        }
    }

    public static class LocusKeyType implements JsonDeserializer<LocusKeyModel>, JsonSerializer<LocusKeyModel> {
        @Override
        public LocusKeyModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String uriString = json.getAsString();
            return new LocusKeyModel(uriString);
        }

        @Override
        public JsonElement serialize(LocusKeyModel src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            return new JsonPrimitive(src.getUrl());
        }
    }
}
