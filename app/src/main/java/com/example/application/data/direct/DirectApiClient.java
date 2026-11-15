package com.example.application.data.direct;

import android.util.Log;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.example.application.data.direct.api.dto.RouteRecommendRequest;
import com.example.application.data.direct.api.dto.RouteRecommendResponse;
import com.example.application.data.direct.common.ExternalServiceUnavailableException;
import com.example.application.data.direct.domain.GeoPoint;
import com.example.application.data.direct.external.kakao.KakaoDirectionsClient;
import com.example.application.data.direct.external.kakao.KakaoLocalClient;
import com.example.application.data.direct.external.kakao.KakaoProperties;
import com.example.application.data.direct.external.kma.KmaProperties;
import com.example.application.data.direct.external.kma.KmaWeatherClient;
import com.example.application.data.direct.place.PlaceService;
import com.example.application.data.direct.routing.RouteRecommendationService;
import com.example.application.data.direct.weather.WeatherService;
import com.example.application.data.remote.ApiClient;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 상세 버전의 "앱 내장 서버". 별도 백엔드 없이 앱이 카카오·기상청 API를 직접 호출하고,
 * 백엔드와 똑같은 분석 로직(경로 특징·난이도 점수·브리핑)을 기기에서 실행한다.
 *
 * <p>{@link ApiClient}와 같은 경로·응답 형식을 쓰므로 경로·장소·날씨 저장소는 서버를 쓰는지
 * 앱이 직접 호출하는지 구분하지 않는다. 오류도 서버와 같은 규칙으로 돌려준다
 * (요청 오류 400 → 안내 그대로 표시, 외부 서비스 오류 503 → 기기 내 대체 로직).
 */
public class DirectApiClient extends ApiClient {
    private static final String TAG = "DirectApi";

    private final Gson gson = new Gson();
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private final RouteRecommendationService routes;
    private final PlaceService places;
    private final WeatherService weather;

    public DirectApiClient(String kakaoRestApiKey, String kmaServiceKey) {
        KakaoProperties kakao = new KakaoProperties(kakaoRestApiKey, null, null);
        KmaProperties kma = new KmaProperties(kmaServiceKey, null);
        routes = new RouteRecommendationService(new KakaoDirectionsClient(kakao));
        places = new PlaceService(new KakaoLocalClient(kakao));
        weather = new WeatherService(new KmaWeatherClient(kma));
    }

    /** 카카오 키가 있어야 상세 버전(경로·장소)을 쓸 수 있다. 기상청 키는 없어도 된다(날씨만 빠짐). */
    public static boolean canRun(String kakaoRestApiKey) {
        return kakaoRestApiKey != null && !kakaoRestApiKey.trim().isEmpty();
    }

    @Override
    public <T> void get(String pathAndQuery, Type responseType, Callback<T> callback) {
        io.execute(() -> deliver(() -> handleGet(pathAndQuery), responseType, callback));
    }

    @Override
    public <T> void post(String path, Object body, Type responseType, Callback<T> callback) {
        io.execute(() -> deliver(() -> handlePost(path, body), responseType, callback));
    }

    private interface Handler {
        Object run() throws IOException;
    }

    private <T> void deliver(Handler handler, Type type, Callback<T> callback) {
        try {
            Object result = handler.run();
            // 서버 응답과 같은 JSON을 거쳐 앱 모델로 바꾼다(필드 이름이 같게 설계돼 있음)
            T value = gson.fromJson(gson.toJson(result), type);
            MainThread.post(() -> callback.onSuccess(value));
        } catch (IllegalArgumentException e) {
            fail(callback, new ApiException(400, "INVALID_REQUEST", e.getMessage()));
        } catch (ExternalServiceUnavailableException e) {
            fail(callback, new ApiException(503, e.code(), e.getMessage()));
        } catch (IOException e) {
            fail(callback, e);
        } catch (RuntimeException e) {
            Log.e(TAG, "unexpected error", e);
            fail(callback, new ApiException(500, "INTERNAL_ERROR", "처리 중 오류가 났어요"));
        }
    }

    private static <T> void fail(Callback<T> callback, Exception e) {
        Log.w(TAG, "request failed: " + e.getMessage());
        MainThread.post(() -> callback.onError(e));
    }

    private Object handlePost(String path, Object body) throws IOException {
        if ("/api/v1/routes/recommend".equals(path)) {
            RouteRecommendRequest req = gson.fromJson(gson.toJson(body), RouteRecommendRequest.class);
            if (req == null || req.origin() == null) {
                throw new IllegalArgumentException("출발 위치가 필요해요");
            }
            return new RouteRecommendResponse(routes.recommend(req));
        }
        throw new IOException("unknown path " + path);
    }

    private Object handleGet(String pathAndQuery) throws IOException {
        int q = pathAndQuery.indexOf('?');
        String path = q < 0 ? pathAndQuery : pathAndQuery.substring(0, q);
        Map<String, String> params = parseQuery(q < 0 ? "" : pathAndQuery.substring(q + 1));
        switch (path) {
            case "/api/v1/places/search":
                return places.search(params.get("q"), point(params));
            case "/api/v1/weather/current": {
                GeoPoint at = point(params);
                if (at == null) {
                    throw new IllegalArgumentException("위치가 필요해요");
                }
                return weather.current(at.latitude(), at.longitude());
            }
            default:
                throw new IOException("unknown path " + path);
        }
    }

    private static GeoPoint point(Map<String, String> params) {
        try {
            return params.containsKey("lat") && params.containsKey("lng")
                    ? new GeoPoint(Double.parseDouble(params.get("lat")), Double.parseDouble(params.get("lng")))
                    : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> out = new HashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            try {
                String k = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), "UTF-8");
                String v = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                out.put(k, v);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }
}
