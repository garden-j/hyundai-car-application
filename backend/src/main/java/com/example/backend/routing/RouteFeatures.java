package com.example.backend.routing;

/**
 * 경로 하나의 난이도 요소. 카카오 안내 지점(guides)과 도로(roads)에서 계산한다.
 *
 * @param congestionRatio 정체·지체 구간 거리 비율(0~1)
 * @param wideRoadRatio   "대로" 구간 거리 비율(넓은 길 판단용 추정치)
 */
public record RouteFeatures(int leftTurns, int rightTurns, int uTurns, int laneChanges, int complexIntersections,
                            int merges, double congestionRatio, double wideRoadRatio) {

    public static RouteFeatures of(com.example.backend.external.kakao.KakaoRoute route) {
        int left = 0, right = 0, uturn = 0, lane = 0, complex = 0, merge = 0;
        for (var g : route.guides()) {
            int t = g.type();
            if (t == GuideTypes.LEFT) {
                left++;
            } else if (t == GuideTypes.RIGHT) {
                right++;
            } else if (t == GuideTypes.U_TURN) {
                uturn++;
            } else if (GuideTypes.isMerge(t)) {
                merge++;
            } else if (GuideTypes.isLanePositioning(t)) {
                lane++;
            } else if (GuideTypes.isComplexIntersection(t)) {
                complex++;
            }
        }
        double total = 0, congested = 0, wide = 0;
        for (var r : route.roads()) {
            total += r.distanceM();
            if (r.trafficState() == 1 || r.trafficState() == 2) {
                congested += r.distanceM();
            }
            if (r.name() != null && r.name().endsWith("대로")) {
                wide += r.distanceM();
            }
        }
        return new RouteFeatures(left, right, uturn, lane, complex, merge,
                total == 0 ? 0 : congested / total, total == 0 ? 0 : wide / total);
    }
}
