package com.example.backend;

import com.example.backend.domain.GeoPoint;
import com.example.backend.external.kakao.KakaoDirectionsParser;
import com.example.backend.external.kakao.KakaoRoute;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.json.JsonMapper;

/** 테스트용 경로 만들기. */
public final class TestRoutes {
    private TestRoutes() {
    }

    public static KakaoRoute sample() throws Exception {
        try (InputStream in = TestRoutes.class.getResourceAsStream("/kakao-directions-sample.json")) {
            return KakaoDirectionsParser.parse(JsonMapper.builder().build().readTree(in)).get(0);
        }
    }

    /** 안내 타입 목록으로 간단한 경로를 만든다(각 지점 500m 간격). */
    public static KakaoRoute withGuides(int distanceM, int durationS, int... types) {
        List<KakaoRoute.Guide> guides = new ArrayList<>();
        guides.add(new KakaoRoute.Guide("출발지", 127.03, 37.49, 0, 0, 100, "출발지"));
        for (int t : types) {
            guides.add(new KakaoRoute.Guide("", 127.03, 37.49, 500, 60, t, ""));
        }
        List<KakaoRoute.Road> roads = List.of(new KakaoRoute.Road("테스트로", distanceM, 4,
                List.of(new GeoPoint(37.49, 127.03), new GeoPoint(37.50, 127.04))));
        return new KakaoRoute(distanceM, durationS, guides, roads, "RECOMMEND");
    }
}
