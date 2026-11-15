package com.example.application.data.direct.routing;

import com.example.application.data.direct.domain.Place;
import com.example.application.data.direct.external.kakao.KakaoRoute;

import java.util.List;

/**
 * 분석이 끝난 경로 후보.
 *
 * @param naviWaypoints Pleos 내비에 넘길 경유지(최대 2개). 같은 경로를 재현하기 위한 지점
 * @param destination   내비 목적지
 */
public record AnalyzedRoute(KakaoRoute route, RouteFeatures features, RouteScorer.Score score,
                            List<Place> naviWaypoints, Place destination) {

    public static AnalyzedRoute of(KakaoRoute route, List<Place> naviWaypoints, Place destination) {
        RouteFeatures f = RouteFeatures.of(route);
        return new AnalyzedRoute(route, f, RouteScorer.score(f), naviWaypoints, destination);
    }

    public boolean sameAs(AnalyzedRoute o) {
        return Math.abs(route.distanceM() - o.route.distanceM()) < 50
                && Math.abs(route.durationS() - o.route.durationS()) < 60;
    }
}
