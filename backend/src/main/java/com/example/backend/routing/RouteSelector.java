package com.example.backend.routing;

import com.example.backend.api.dto.RouteCandidateDto;
import com.example.backend.api.dto.RouteRecommendRequest;
import com.example.backend.domain.Enums.Difficulty;
import com.example.backend.domain.Enums.DrivingSkill;
import com.example.backend.domain.Enums.RoadEnvironment;
import com.example.backend.domain.Enums.RouteKind;
import com.example.backend.domain.GeoPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 실제 경로 후보들 중 화면에 보여줄 최대 3개를 고른다.
 * 넓은 길(난이도 최저) / 최단 거리 / 목표 연습(선택한 기술을 가장 많이 경험) 순서이며,
 * 난이도가 "도전"이고 긴장도가 높지 않으면 목표 연습 경로를 맨 앞에 둔다(앱 규칙과 동일).
 */
public final class RouteSelector {
    static final int MAX_PREVIEW_POINTS = 150;

    private RouteSelector() {
    }

    public static List<RouteCandidateDto> select(List<AnalyzedRoute> analyzed, RouteRecommendRequest req,
                                                 GeoPoint start) {
        List<AnalyzedRoute> pool = dedupe(analyzed);
        if (pool.isEmpty()) {
            return List.of();
        }
        AnalyzedRoute easy = pool.stream()
                .min(Comparator.comparingInt((AnalyzedRoute a) -> a.score().value())
                        .thenComparingDouble(a -> a.features().congestionRatio()))
                .orElseThrow();
        pool.remove(easy);
        AnalyzedRoute shortest = pool.stream()
                .min(Comparator.comparingInt(a -> a.route().distanceM())).orElse(null);
        if (shortest != null) {
            pool.remove(shortest);
        }
        AnalyzedRoute goal = pool.stream()
                .max(Comparator.comparingInt((AnalyzedRoute a) -> goalMatch(a.features(), req))
                        .thenComparing(a -> -a.features().congestionRatio()))
                .orElse(null);

        List<RouteCandidateDto> picked = new ArrayList<>();
        boolean challengeFirst = req.effectiveDifficulty() == Difficulty.CHALLENGE && !req.isHighTension();
        if (challengeFirst && goal != null) {
            picked.add(toDto(goal, RouteKind.GOAL_FOCUSED, req, start));
        }
        picked.add(toDto(easy, RouteKind.WIDE_EASY, req, start));
        if (shortest != null) {
            RouteCandidateDto dto = toDto(shortest, RouteKind.SHORTEST, req, start);
            if (shortest.route().distanceM() >= easy.route().distanceM()) {
                // 넓은 길 후보가 더 짧으면 "최단 거리"라고 부르지 않는다.
                dto = withTitle(dto, "다른 길로 가요");
            }
            picked.add(dto);
        }
        if (!challengeFirst && goal != null) {
            picked.add(toDto(goal, RouteKind.GOAL_FOCUSED, req, start));
        }
        List<RouteCandidateDto> out = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            RouteCandidateDto d = picked.get(i);
            out.add(new RouteCandidateDto("r" + (i + 1), d.kind(), picked.size() == 1 ? "추천 경로예요" : d.title(),
                    d.reason(), d.minutes(), d.distanceKm(), d.laneChanges(), d.complexIntersections(), d.merges(),
                    d.uTurns(), d.leftTurns(), d.parkingSpots(), d.difficultyScore(), d.scoreFactors(), d.origin(),
                    d.destination(), d.waypoints(), d.previewPath(), d.briefing(), d.source()));
        }
        return out;
    }

    private static RouteCandidateDto withTitle(RouteCandidateDto d, String title) {
        return new RouteCandidateDto(d.id(), d.kind(), title, d.reason(), d.minutes(), d.distanceKm(),
                d.laneChanges(), d.complexIntersections(), d.merges(), d.uTurns(), d.leftTurns(), d.parkingSpots(),
                d.difficultyScore(), d.scoreFactors(), d.origin(), d.destination(), d.waypoints(), d.previewPath(),
                d.briefing(), d.source());
    }

    static List<AnalyzedRoute> dedupe(List<AnalyzedRoute> in) {
        List<AnalyzedRoute> out = new ArrayList<>();
        for (AnalyzedRoute a : in) {
            if (out.stream().noneMatch(a::sameAs)) {
                out.add(a);
            }
        }
        return out;
    }

    /** 선택한 기술·환경을 경로에서 몇 번 경험하는지. */
    static int goalMatch(RouteFeatures f, RouteRecommendRequest req) {
        int n = 0;
        for (DrivingSkill s : req.skillsOrEmpty()) {
            n += switch (s) {
                case LEFT_TURN -> f.leftTurns();
                case RIGHT_TURN -> f.rightTurns();
                case U_TURN -> f.uTurns();
                case LANE_CHANGE -> f.laneChanges();
                case INTERSECTION -> f.complexIntersections() + f.leftTurns();
                case MERGE -> f.merges();
                case PARKING -> 0;
            };
        }
        if (req.environmentsOrEmpty().contains(RoadEnvironment.COMPLEX_INTERSECTION)) {
            n += f.complexIntersections();
        }
        return n;
    }

    static RouteCandidateDto toDto(AnalyzedRoute a, RouteKind kind, RouteRecommendRequest req, GeoPoint start) {
        RouteFeatures f = a.features();
        String title;
        String reason;
        switch (kind) {
            case WIDE_EASY -> {
                title = "넓은 길로 가요";
                reason = f.wideRoadRatio() >= 0.4 ? "큰길 위주라 도로가 넓어요"
                        : f.laneChanges() <= 1 ? "차선 변경이 적고 단순한 길이에요" : "복잡한 구간이 가장 적어요";
            }
            case SHORTEST -> {
                title = "최단 거리예요";
                int turns = f.leftTurns() + f.rightTurns() + f.complexIntersections();
                reason = turns > 0 ? "회전 구간이 " + turns + "곳 있어요" : "단순한 길이에요";
            }
            default -> {
                int match = goalMatch(f, req);
                if (match > 0) {
                    title = "목표 연습 최적화";
                    reason = goalReason(f, req);
                } else {
                    title = "한산한 길 위주";
                    reason = f.congestionRatio() < 0.1 ? "지금 막히는 구간이 거의 없어요" : "다른 길보다 덜 막혀요";
                }
            }
        }
        boolean parking = req.skillsOrEmpty().contains(DrivingSkill.PARKING) && kind == RouteKind.GOAL_FOCUSED;
        return new RouteCandidateDto(null, kind, title, reason,
                Math.max(1, Math.round(a.route().durationS() / 60f)),
                Math.round(a.route().distanceM() / 100.0) / 10.0,
                f.laneChanges(), f.complexIntersections(), f.merges(), f.uTurns(), f.leftTurns(), parking ? 1 : 0,
                a.score().value(), a.score().factors(), start, a.destination(), a.naviWaypoints(),
                preview(a), BriefingBuilder.build(a.route()), "KAKAO");
    }

    private static String goalReason(RouteFeatures f, RouteRecommendRequest req) {
        for (DrivingSkill s : req.skillsOrEmpty()) {
            switch (s) {
                case U_TURN -> { if (f.uTurns() > 0) return "유턴 지점이 " + f.uTurns() + "곳 있어요"; }
                case LEFT_TURN -> { if (f.leftTurns() > 0) return "좌회전 지점이 " + f.leftTurns() + "곳 있어요"; }
                case RIGHT_TURN -> { if (f.rightTurns() > 0) return "우회전 지점이 " + f.rightTurns() + "곳 있어요"; }
                case LANE_CHANGE -> { if (f.laneChanges() > 0) return "차로를 맞출 곳이 " + f.laneChanges() + "곳 있어요"; }
                case INTERSECTION -> { if (f.complexIntersections() + f.leftTurns() > 0) return "교차로를 " + (f.complexIntersections() + f.leftTurns()) + "번 지나요"; }
                case MERGE -> { if (f.merges() > 0) return "합류 구간이 " + f.merges() + "곳 있어요"; }
                default -> { }
            }
        }
        return "복잡한 교차로를 " + f.complexIntersections() + "번 지나요";
    }

    /** 도로 형상 점을 이어 미리보기 선을 만들고, 점이 너무 많으면 고르게 줄인다. */
    static List<GeoPoint> preview(AnalyzedRoute a) {
        List<GeoPoint> all = new ArrayList<>();
        for (var r : a.route().roads()) {
            all.addAll(r.path());
        }
        if (all.size() <= MAX_PREVIEW_POINTS) {
            return all;
        }
        List<GeoPoint> out = new ArrayList<>();
        double step = (all.size() - 1) / (double) (MAX_PREVIEW_POINTS - 1);
        for (int i = 0; i < MAX_PREVIEW_POINTS; i++) {
            out.add(all.get((int) Math.round(i * step)));
        }
        return out;
    }
}
