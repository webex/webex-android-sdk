package com.cisco.spark.android.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

import static com.cisco.spark.android.util.DateUtils.buildIso8601Format;

public class DateTypeAdapter implements JsonSerializer<Date> {

    private ThreadLocal<DateFormat> threadLocalDateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return buildIso8601Format();
        }
    };

    @Override
    public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        if (date == null)
            return null;
        return new JsonPrimitive(threadLocalDateFormat.get().format(date));
    }
}
