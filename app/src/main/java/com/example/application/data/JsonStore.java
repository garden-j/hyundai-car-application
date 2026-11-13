package com.example.application.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * 작은 앱 데이터(프로필·동의 이력·세션 스냅샷)를 SharedPreferences에 JSON으로 저장한다.
 * 주행 중 네트워크가 끊겨도 동작해야 하는 데이터는 모두 기기에 둔다.
 */
public class JsonStore {
    private static final String TAG = "JsonStore";
    private final SharedPreferences prefs;
    private final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

    public JsonStore(Context context, String name) {
        this.prefs = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public <T> T read(String key, Type type) {
        String json = prefs.getString(key, null);
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, type);
        } catch (JsonParseException e) {
            Log.w(TAG, "corrupted value for " + key + ", ignoring", e);
            return null;
        }
    }

    public void write(String key, Object value) {
        prefs.edit().putString(key, gson.toJson(value)).apply();
    }

    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }
}
