package com.johnymuffin.jban.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Null tolerant readers for Gson objects. Every getter treats a missing key and a
 * JSON null the same way, which is what the API payloads and config files expect.
 */
public class JsonUtil {

    private JsonUtil() {
    }

    public static boolean has(JsonObject jsonObject, String key) {
        if (jsonObject == null) {
            return false;
        }
        JsonElement element = jsonObject.get(key);
        return element != null && !element.isJsonNull();
    }

    public static String getString(JsonObject jsonObject, String key) {
        return has(jsonObject, key) ? jsonObject.get(key).getAsString() : null;
    }

    public static String getString(JsonObject jsonObject, String key, String defaultValue) {
        return has(jsonObject, key) ? jsonObject.get(key).getAsString() : defaultValue;
    }

    public static long getLong(JsonObject jsonObject, String key, long defaultValue) {
        return has(jsonObject, key) ? jsonObject.get(key).getAsLong() : defaultValue;
    }

    public static int getInt(JsonObject jsonObject, String key, int defaultValue) {
        return has(jsonObject, key) ? jsonObject.get(key).getAsInt() : defaultValue;
    }

    public static boolean getBoolean(JsonObject jsonObject, String key, boolean defaultValue) {
        return has(jsonObject, key) ? jsonObject.get(key).getAsBoolean() : defaultValue;
    }

    public static JsonArray getArray(JsonObject jsonObject, String key) {
        if (has(jsonObject, key) && jsonObject.get(key).isJsonArray()) {
            return jsonObject.getAsJsonArray(key);
        }
        return new JsonArray();
    }

    public static JsonObject getObject(JsonArray jsonArray, int index) {
        JsonElement element = jsonArray.get(index);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
    
    public static JsonObject parseObject(String json) {
        JsonElement element = JsonParser.parseString(json);
        return element.getAsJsonObject();
    }
}
