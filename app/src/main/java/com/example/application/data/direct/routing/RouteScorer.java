package com.example.application.data.direct.routing;

import java.util.ArrayList;
import java.util.List;

/**
 * 난이도 점수(0~100)와 산출 근거. 가중치는 앱(RouteDifficultyScorer)과 같다:
 * 복잡 교차로 10 > 합류 8 > 유턴 6 > 차로 맞추기 4 > 좌회전 3. 정체 구간 비율도 반영한다.
 */
public final class RouteScorer {
    public static final int W_COMPLEX = 10;
    public static final int W_MERGE = 8;
    public static final int W_U_TURN = 6;
    public static final int W_LANE = 4;
    public static final int W_LEFT = 3;
    /** 정체 비율 100%일 때 더하는 점수. */
    public static final int W_CONGESTION = 15;

    public record Score(int value, List<String> factors) {
    }

    private RouteScorer() {
    }

    public static Score score(RouteFeatures f) {
        int congestion = (int) Math.round(f.congestionRatio() * W_CONGESTION);
        int v = f.complexIntersections() * W_COMPLEX + f.merges() * W_MERGE + f.uTurns() * W_U_TURN
                + f.laneChanges() * W_LANE + f.leftTurns() * W_LEFT + congestion;
        List<String> factors = new ArrayList<>();
        add(factors, f.complexIntersections(), "복잡한 교차로", W_COMPLEX);
        add(factors, f.merges(), "합류 구간", W_MERGE);
        add(factors, f.uTurns(), "유턴", W_U_TURN);
        add(factors, f.laneChanges(), "차로 변경", W_LANE);
        add(factors, f.leftTurns(), "좌회전", W_LEFT);
        if (congestion > 0) {
            factors.add("막히는 구간 " + Math.round(f.congestionRatio() * 100) + "% (+" + congestion + ")");
        }
        if (factors.isEmpty()) {
            factors.add("복잡한 구간이 없어요");
        }
        return new Score(Math.min(100, v), factors);
    }

    private static void add(List<String> f, int count, String name, int weight) {
        if (count > 0) {
            f.add(name + " " + count + "곳 (+" + count * weight + ")");
        }
    }
}
