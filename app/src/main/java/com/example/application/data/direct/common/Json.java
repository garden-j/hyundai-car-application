package com.example.application.data.direct.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Gson 트리를 Jackson의 path()처럼 안전하게 읽는 도우미(없는 값은 기본값). */
public final class Json {
    private Json() {
    }

    public static JsonObject obj(JsonElement e, String key) {
        if (e != null && e.isJsonObject()) {
            JsonElement v = e.getAsJsonObject().get(key);
            if (v != null && v.isJsonObject()) {
                return v.getAsJsonObject();
            }
        }
        return new JsonObject();
    }

    public static JsonArray arr(JsonElement e, String key) {
        if (e != null && e.isJsonObject()) {
            JsonElement v = e.getAsJsonObject().get(key);
            if (v != null && v.isJsonArray()) {
                return v.getAsJsonArray();
            }
        }
        return new JsonArray();
    }

    private static JsonElement prim(JsonElement e, String key) {
        if (e != null && e.isJsonObject()) {
            JsonElement v = e.getAsJsonObject().get(key);
            if (v != null && v.isJsonPrimitive()) {
                return v;
            }
        }
        return null;
    }

    public static String str(JsonElement e, String key, String def) {
        JsonElement v = prim(e, key);
        return v == null ? def : v.getAsString();
    }

    public static int integer(JsonElement e, String key, int def) {
        JsonElement v = prim(e, key);
        try {
            return v == null ? def : (int) Math.round(v.getAsDouble());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public static double dbl(JsonElement e, String key, double def) {
        JsonElement v = prim(e, key);
        try {
            return v == null ? def : v.getAsDouble();
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
