package com.example.application.data.remote;

import android.util.Log;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 백엔드 HTTP 클라이언트(추가 라이브러리 없이 HttpURLConnection + Gson).
 * 짧은 타임아웃을 둬서 서버가 느리면 바로 기기 내 대체 로직으로 넘어가게 한다.
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    static final int CONNECT_TIMEOUT_MS = 3000;
    static final int READ_TIMEOUT_MS = 8000;

    /** 서버가 돌려준 오류(code는 ROUTING_UNAVAILABLE 등 고정 문자열). */
    public static class ApiException extends IOException {
        public final int status;
        public final String code;

        ApiException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    private final String baseUrl;
    private final Gson gson = new Gson();
    private final ExecutorService io = Executors.newFixedThreadPool(2);

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public <T> void get(String pathAndQuery, Type responseType, Callback<T> callback) {
        io.execute(() -> deliver(() -> request("GET", pathAndQuery, null, responseType), callback));
    }

    public <T> void post(String path, Object body, Type responseType, Callback<T> callback) {
        io.execute(() -> deliver(() -> request("POST", path, gson.toJson(body), responseType), callback));
    }

    private interface Call<T> {
        T run() throws IOException;
    }

    private static <T> void deliver(Call<T> call, Callback<T> callback) {
        try {
            T value = call.run();
            MainThread.post(() -> callback.onSuccess(value));
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "request failed: " + e.getMessage());
            MainThread.post(() -> callback.onError(e instanceof Exception ? e : new IOException(e)));
        }
    }

    private <T> T request(String method, String path, String json, Type type) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        try {
            c.setRequestMethod(method);
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setRequestProperty("Accept", "application/json");
            if (json != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = c.getOutputStream()) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = c.getResponseCode();
            String body = read(status >= 400 ? c.getErrorStream() : c.getInputStream());
            if (status >= 400) {
                String code = "HTTP_" + status;
                String message = body;
                try {
                    JsonObject err = gson.fromJson(body, JsonObject.class);
                    code = err.get("code").getAsString();
                    message = err.get("message").getAsString();
                } catch (RuntimeException ignored) {
                    // 오류 본문이 JSON이 아니면 그대로 둔다
                }
                throw new ApiException(status, code, message);
            }
            return gson.fromJson(body, type);
        } finally {
            c.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream s = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = s.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        }
    }
}
