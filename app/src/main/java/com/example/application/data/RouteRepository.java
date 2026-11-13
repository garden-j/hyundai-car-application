package com.example.application.data;

import com.example.application.core.Callback;
import com.example.application.model.BriefingPoint;
import com.example.application.model.DriverProfile;
import com.example.application.model.GeoPoint;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RouteCandidate;

import java.util.List;

/**
 * 연습 경로 추천·복잡 구간 브리핑. 지금은 기기 내 {@link LocalRouteRepository},
 * 백엔드 연동 후에는 같은 인터페이스의 원격 구현으로 교체한다.
 */
public interface RouteRepository {
    void recommend(GeoPoint origin, PracticeSetup setup, DriverProfile profile,
                   Callback<List<RouteCandidate>> callback);

    void briefing(RouteCandidate route, Callback<List<BriefingPoint>> callback);
}
