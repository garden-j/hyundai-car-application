package com.example.application.data.direct.routing;

import com.example.application.data.direct.api.dto.BriefingPointDto;
import com.example.application.data.direct.common.Josa;
import com.example.application.data.direct.domain.Enums.BriefingType;
import com.example.application.data.direct.external.kakao.KakaoRoute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 실제 경로의 안내 지점으로 출발 전 복잡 구간 브리핑을 만든다.
 * 부담이 큰 지점부터 최대 {@link #MAX_POINTS}곳을 고른 뒤 주행 순서대로 정렬한다.
 */
public final class BriefingBuilder {
    public static final int MAX_POINTS = 5;

    private BriefingBuilder() {
    }

    private record Candidate(BriefingType type, int weight, double distanceKm, KakaoRoute.Guide guide) {
    }

    public static List<BriefingPointDto> build(KakaoRoute route) {
        List<Candidate> all = new ArrayList<>();
        double cumulativeM = 0;
        for (KakaoRoute.Guide g : route.guides()) {
            cumulativeM += g.distanceM();
            BriefingType type = classify(g.type());
            if (type == null) {
                continue;
            }
            all.add(new Candidate(type, weight(type), cumulativeM / 1000.0, g));
        }
        List<Candidate> picked = all.stream()
                .sorted(Comparator.comparingInt(Candidate::weight).reversed()
                        .thenComparingDouble(Candidate::distanceKm))
                .limit(MAX_POINTS)
                .sorted(Comparator.comparingDouble(Candidate::distanceKm))
                .collect(java.util.stream.Collectors.toList());
        List<BriefingPointDto> out = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            Candidate c = picked.get(i);
            String where = c.guide().name() == null || c.guide().name().trim().isEmpty() ? null : c.guide().name();
            String instruction = instruction(c.type(), where, c.guide().guidance());
            String speech = spokenDistance(c.distanceKm()) + " 앞, " + instruction + ". " + tip(c.type());
            out.add(new BriefingPointDto(i + 1, c.type(), Math.round(c.distanceKm() * 10) / 10.0,
                    instruction, speech, c.guide().point()));
        }
        return out;
    }

    static BriefingType classify(int type) {
        if (GuideTypes.isComplexIntersection(type)) {
            return BriefingType.COMPLEX_INTERSECTION;
        }
        if (GuideTypes.isMerge(type)) {
            return BriefingType.MERGE;
        }
        if (type == GuideTypes.U_TURN) {
            return BriefingType.U_TURN;
        }
        if (GuideTypes.isLanePositioning(type)) {
            return BriefingType.LANE_CHANGE_PREP;
        }
        if (type == GuideTypes.LEFT) {
            return BriefingType.LEFT_TURN;
        }
        return null;
    }

    static int weight(BriefingType t) {
        return switch (t) {
            case COMPLEX_INTERSECTION -> RouteScorer.W_COMPLEX;
            case MERGE -> RouteScorer.W_MERGE;
            case U_TURN -> RouteScorer.W_U_TURN;
            case LANE_CHANGE_PREP -> RouteScorer.W_LANE;
            case LEFT_TURN -> RouteScorer.W_LEFT;
            case PARKING -> 1;
        };
    }

    private static String instruction(BriefingType type, String where, String guidance) {
        String action = guidance == null || guidance.trim().isEmpty() ? defaultAction(type) : guidance;
        return where == null ? action : Josa.eseo(where) + " " + action;
    }

    private static String defaultAction(BriefingType t) {
        return switch (t) {
            case COMPLEX_INTERSECTION -> "복잡한 교차로 통과";
            case MERGE -> "본선 합류";
            case U_TURN -> "유턴";
            case LANE_CHANGE_PREP -> "차로 맞추기";
            case LEFT_TURN -> "좌회전";
            case PARKING -> "주차";
        };
    }

    private static String tip(BriefingType t) {
        return switch (t) {
            case COMPLEX_INTERSECTION -> "길이 여러 갈래로 나뉘어요. 바닥의 화살표를 보고 가야 할 방향의 차로를 유지하세요.";
            case MERGE -> "합류 차로가 끝나기 전에 본선 차량 흐름을 확인하고, 사이드미러로 간격을 본 뒤 들어가세요.";
            case U_TURN -> "1차로에서 유턴 표지와 신호를 확인하고, 맞은편 차가 끊길 때 크게 돌아 주세요.";
            case LANE_CHANGE_PREP -> "미리 방향지시등을 켜고 사이드미러와 사각지대를 확인한 뒤 차로를 옮기세요.";
            case LEFT_TURN -> "좌회전 신호를 확인하고, 비보호라면 맞은편 직진 차량이 모두 지나간 뒤 돌아 주세요.";
            case PARKING -> "도착하면 비어 있는 주차 칸을 먼저 찾고 천천히 주차하세요.";
        };
    }

    public static String spokenDistance(double km) {
        if (km < 1.0) {
            return Math.max(100, (int) (Math.round(km * 10) * 100)) + "미터";
        }
        return String.format(Locale.KOREA, "%.1f킬로미터", km);
    }
}
