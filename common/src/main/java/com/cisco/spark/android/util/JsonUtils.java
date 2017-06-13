package com.cisco.spark.android.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonUtils {
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
