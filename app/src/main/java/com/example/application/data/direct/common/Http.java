package com.example.application.data.direct.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/** 외부 API GET 호출(HttpURLConnection). 호출한 스레드에서 블로킹으로 동작하므로 백그라운드에서 부른다. */
public final class Http {
    static final int CONNECT_TIMEOUT_MS = 4000;
    static final int READ_TIMEOUT_MS = 8000;

    private Http() {
    }

    /** 쿼리 문자열 빌더. 값은 UTF-8로 인코딩한다. */
    public static final class Query {
        private final String base;
        private final Map<String, String> params = new LinkedHashMap<>();

        public Query(String base) {
            this.base = base;
        }

        public Query param(String name, Object value) {
            params.put(name, String.valueOf(value));
            return this;
        }

        public String build() {
            StringBuilder sb = new StringBuilder(base);
            char sep = base.contains("?") ? '&' : '?';
            for (Map.Entry<String, String> e : params.entrySet()) {
                sb.append(sep).append(encode(e.getKey())).append('=').append(encode(e.getValue()));
                sep = '&';
            }
            return sb.toString();
        }
    }

    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** GET 후 JSON으로 파싱한다. 실패(네트워크·4xx·5xx·JSON 아님)는 IOException. */
    public static JsonElement getJson(String url, String authorization) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setRequestProperty("Accept", "application/json");
            if (authorization != null) {
                c.setRequestProperty("Authorization", authorization);
            }
            int status = c.getResponseCode();
            String body = read(status >= 400 ? c.getErrorStream() : c.getInputStream());
            if (status >= 400) {
                throw new IOException("HTTP " + status + ": " + (body.length() > 200 ? body.substring(0, 200) : body));
            }
            try {
                return JsonParser.parseString(body);
            } catch (RuntimeException e) {
                throw new IOException("invalid JSON response", e);
            }
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
