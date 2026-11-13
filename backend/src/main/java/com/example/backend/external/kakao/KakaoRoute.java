package com.example.backend.external.kakao;

import com.example.backend.domain.GeoPoint;

import java.util.List;

/**
 * 카카오모빌리티 길찾기 결과 경로 하나(필요한 필드만).
 *
 * @param distanceM 전체 거리(미터)
 * @param durationS 전체 소요 시간(초)
 * @param guides    모든 섹션의 안내 지점을 순서대로 이은 목록
 * @param roads     모든 섹션의 도로 목록
 */
public record KakaoRoute(int distanceM, int durationS, List<Guide> guides, List<Road> roads, String priority) {

    /**
     * 안내 지점.
     *
     * @param distanceM 이전 안내 지점부터 이 지점까지 거리(미터)
     * @param type      안내 타입 코드(1 좌회전, 3 유턴, 10 고속도로 입구 …)
     */
    public record Guide(String name, double x, double y, int distanceM, int durationS, int type, String guidance) {
        public GeoPoint point() {
            return new GeoPoint(y, x);
        }
    }

    /**
     * @param trafficState 0 정보 없음, 1 정체, 2 지체, 3 서행, 4 원활, 6 사고(통행 불가)
     * @param path         도로 형상 점들
     */
    public record Road(String name, int distanceM, int trafficState, List<GeoPoint> path) {
    }
}
