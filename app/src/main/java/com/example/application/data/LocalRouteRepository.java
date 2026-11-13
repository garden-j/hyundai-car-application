package com.example.application.data;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;
import com.example.application.domain.BriefingGenerator;
import com.example.application.domain.RouteCandidateGenerator;
import com.example.application.model.BriefingPoint;
import com.example.application.model.DriverProfile;
import com.example.application.model.GeoPoint;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RouteCandidate;

import java.util.List;

/** 기기 내 경로 추천·브리핑. 서버 응답처럼 비동기로 결과를 준다. */
public class LocalRouteRepository implements RouteRepository {
    private static final long LATENCY_MS = 400;

    @Override
    public void recommend(GeoPoint origin, PracticeSetup setup, DriverProfile profile,
                          Callback<List<RouteCandidate>> callback) {
        List<RouteCandidate> routes = RouteCandidateGenerator.generate(origin, setup);
        MainThread.postDelayed(() -> callback.onSuccess(routes), LATENCY_MS);
    }

    @Override
    public void briefing(RouteCandidate route, Callback<List<BriefingPoint>> callback) {
        List<BriefingPoint> points = BriefingGenerator.generate(route);
        MainThread.postDelayed(() -> callback.onSuccess(points), LATENCY_MS / 2);
    }
}
