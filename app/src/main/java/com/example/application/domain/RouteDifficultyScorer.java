package com.example.application.domain;

import com.example.application.model.RouteCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * 경로 난이도 점수(0~100)와 산출 근거를 만든다(기능 명세 "경로 난이도 설명": 점수 산출 근거 공개).
 * 가중치는 초보 운전자 인터뷰 기준 체감 부담 순서(복잡 교차로 > 합류 > 유턴 > 차로 변경 > 좌회전)로 정했다.
 */
public final class RouteDifficultyScorer {
    public static final int W_COMPLEX_INTERSECTION = 10;
    public static final int W_MERGE = 8;
    public static final int W_U_TURN = 6;
    public static final int W_LANE_CHANGE = 4;
    public static final int W_LEFT_TURN = 3;
    public static final int W_PARKING = 5;

    private RouteDifficultyScorer() {
    }

    public static void score(RouteCandidate r) {
        int score = r.complexIntersections * W_COMPLEX_INTERSECTION
                + r.merges * W_MERGE
                + r.uTurns * W_U_TURN
                + r.laneChanges * W_LANE_CHANGE
                + r.leftTurns * W_LEFT_TURN
                + r.parkingSpots * W_PARKING;
        r.difficultyScore = Math.min(100, score);

        List<String> f = new ArrayList<>();
        add(f, r.complexIntersections, "복잡한 교차로", W_COMPLEX_INTERSECTION);
        add(f, r.merges, "합류 구간", W_MERGE);
        add(f, r.uTurns, "유턴", W_U_TURN);
        add(f, r.laneChanges, "차로 변경", W_LANE_CHANGE);
        add(f, r.leftTurns, "좌회전", W_LEFT_TURN);
        add(f, r.parkingSpots, "주차", W_PARKING);
        if (f.isEmpty()) {
            f.add("복잡한 구간이 없어요");
        }
        r.scoreFactors = f;
    }

    private static void add(List<String> f, int count, String name, int weight) {
        if (count > 0) {
            f.add(name + " " + count + "곳 (+" + (count * weight) + ")");
        }
    }

    public static String levelLabel(int score) {
        if (score < 25) {
            return "쉬움";
        }
        if (score < 55) {
            return "보통";
        }
        return "어려움";
    }
}
