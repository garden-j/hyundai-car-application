package com.example.application.data.remote;

import android.util.Log;

import com.example.application.core.Callback;
import com.example.application.data.RouteRepository;
import com.example.application.model.BriefingPoint;
import com.example.application.model.DriverProfile;
import com.example.application.model.GeoPoint;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RouteCandidate;

import java.util.List;

/**
 * 서버 경로 추천(실제 도로 기반). 서버 오류·키 미설정(503)·네트워크 끊김이면 기기 내 추천으로 대체한다.
 * 단, 목적지가 너무 먼 등 요청 자체가 잘못된 경우(400)는 그 안내를 그대로 보여준다.
 */
public class RemoteRouteRepository implements RouteRepository {
    private static final String TAG = "RemoteRoutes";

    static class Response {
        List<RouteCandidate> routes;
    }

    private final ApiClient api;
    private final RouteRepository fallback;

    public RemoteRouteRepository(ApiClient api, RouteRepository fallback) {
        this.api = api;
        this.fallback = fallback;
    }

    @Override
    public void recommend(GeoPoint origin, PracticeSetup setup, DriverProfile profile,
                          Callback<List<RouteCandidate>> callback) {
        api.post("/api/v1/routes/recommend", RouteRequestBody.from(origin, setup), Response.class,
                new Callback<Response>() {
                    @Override
                    public void onSuccess(Response value) {
                        if (value == null || value.routes == null || value.routes.isEmpty()) {
                            fallback.recommend(origin, setup, profile, callback);
                        } else {
                            callback.onSuccess(value.routes);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (e instanceof ApiClient.ApiException && ((ApiClient.ApiException) e).status == 400) {
                            callback.onError(e);
                            return;
                        }
                        Log.i(TAG, "server routes unavailable, using on-device routes: " + e.getMessage());
                        fallback.recommend(origin, setup, profile, callback);
                    }
                });
    }

    @Override
    public void briefing(RouteCandidate route, Callback<List<BriefingPoint>> callback) {
        if (route.briefing != null) {
            callback.onSuccess(route.briefing);
        } else {
            fallback.briefing(route, callback);
        }
    }
}
