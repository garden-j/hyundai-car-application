package com.example.backend.routing;

import com.example.backend.api.dto.RouteCandidateDto;
import com.example.backend.api.dto.RouteRecommendRequest;
import com.example.backend.common.ExternalServiceUnavailableException;
import com.example.backend.domain.Enums.Difficulty;
import com.example.backend.domain.Enums.DrivingSkill;
import com.example.backend.domain.GeoPoint;
import com.example.backend.domain.Place;
import com.example.backend.external.kakao.KakaoDirectionsClient;
import com.example.backend.external.kakao.KakaoDirectionsClient.Query;
import com.example.backend.external.kakao.KakaoRoute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 연습 경로 추천.
 * <ul>
 *   <li>목적지가 있으면: 추천·최단거리·(유턴 회피 또는 최단시간) 조건으로 실제 경로를 받아 비교</li>
 *   <li>목적지가 없으면: 출발지에서 돌아오는 연습 코스 3개(방향이 다른 삼각 코스)를 받아 비교</li>
 * </ul>
 */
@Service
public class RouteRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(RouteRecommendationService.class);
    /** 연습 목적지로 현실적인 최대 직선 거리(km). */
    public static final double MAX_PRACTICE_KM = 100;
    static final double[] COURSE_BEARINGS = {30, 150, 270};

    private final KakaoDirectionsClient directions;

    public RouteRecommendationService(KakaoDirectionsClient directions) {
        this.directions = directions;
    }

    public List<RouteCandidateDto> recommend(RouteRecommendRequest req) {
        if (!directions.available()) {
            throw new ExternalServiceUnavailableException("ROUTING_UNAVAILABLE", "카카오 REST API 키가 설정되지 않았어요");
        }
        GeoPoint start = req.start();
        List<AnalyzedRoute> analyzed = req.hasDestination() ? destinationRoutes(req, start) : courseRoutes(req, start);
        if (analyzed.isEmpty()) {
            throw new ExternalServiceUnavailableException("ROUTING_NO_RESULT", "조건에 맞는 경로를 찾지 못했어요");
        }
        return RouteSelector.select(analyzed, req, start);
    }

    private List<AnalyzedRoute> destinationRoutes(RouteRecommendRequest req, GeoPoint start) {
        Place dest = req.destination().destination();
        double km = start.distanceKmTo(dest.point());
        if (km > MAX_PRACTICE_KM) {
            throw new IllegalArgumentException(String.format(
                    "목적지가 출발지에서 약 %.0fkm 떨어져 있어요. 출발지를 확인해 주세요.", km));
        }
        boolean round = req.destination().roundTrip();
        GeoPoint target = round ? start : dest.point();
        List<GeoPoint> via = round ? List.of(dest.point()) : List.of();
        Place naviDest = round ? new Place("출발지", "", start) : dest;
        List<Place> naviWaypoints = round ? List.of(dest) : List.of();
        boolean wantsUTurn = req.skillsOrEmpty().contains(DrivingSkill.U_TURN);

        List<Query> queries = List.of(
                new Query(start, target, via, "RECOMMEND", null, true),
                new Query(start, target, via, "DISTANCE", null, false),
                // 유턴 연습이 목표가 아니면 유턴 없는 길도 후보로(초보에게 부담 적음)
                wantsUTurn ? new Query(start, target, via, "TIME", null, false)
                        : new Query(start, target, via, "RECOMMEND", "uturn", false));
        return runAll(queries, r -> AnalyzedRoute.of(r, naviWaypoints, naviDest));
    }

    private List<AnalyzedRoute> courseRoutes(RouteRecommendRequest req, GeoPoint start) {
        double km = targetKm(req);
        double side = km / 3.0;
        List<Query> queries = new ArrayList<>();
        List<List<Place>> coursePoints = new ArrayList<>();
        for (double bearing : COURSE_BEARINGS) {
            GeoPoint p1 = start.offsetKm(side * Math.cos(Math.toRadians(bearing)), side * Math.sin(Math.toRadians(bearing)));
            GeoPoint p2 = start.offsetKm(side * Math.cos(Math.toRadians(bearing + 60)), side * Math.sin(Math.toRadians(bearing + 60)));
            queries.add(new Query(start, start, List.of(p1, p2), "RECOMMEND", null, false));
            coursePoints.add(List.of(new Place("연습 코스 지점 1", "", p1), new Place("연습 코스 지점 2", "", p2)));
        }
        Place home = new Place("출발지", "", start);
        List<AnalyzedRoute> out = new ArrayList<>();
        List<CompletableFuture<List<KakaoRoute>>> futures = queries.stream()
                .map(q -> CompletableFuture.supplyAsync(() -> safe(q))).toList();
        for (int i = 0; i < futures.size(); i++) {
            for (KakaoRoute r : futures.get(i).join()) {
                out.add(AnalyzedRoute.of(r, coursePoints.get(i), home));
            }
        }
        return out;
    }

    static double targetKm(RouteRecommendRequest req) {
        if (req.amount() != null) {
            return Math.max(1.0, req.amount().estimatedKm());
        }
        Difficulty d = req.effectiveDifficulty();
        return d == Difficulty.EASY ? 4.0 : d == Difficulty.CHALLENGE ? 8.0 : 6.0;
    }

    private List<AnalyzedRoute> runAll(List<Query> queries, Function<KakaoRoute, AnalyzedRoute> analyze) {
        List<CompletableFuture<List<KakaoRoute>>> futures = queries.stream()
                .map(q -> CompletableFuture.supplyAsync(() -> safe(q))).toList();
        List<AnalyzedRoute> out = new ArrayList<>();
        for (CompletableFuture<List<KakaoRoute>> f : futures) {
            for (KakaoRoute r : f.join()) {
                out.add(analyze.apply(r));
            }
        }
        return out;
    }

    /** 일부 조건이 실패해도 나머지 후보로 추천할 수 있게 개별 실패는 무시한다. */
    private List<KakaoRoute> safe(Query q) {
        try {
            return directions.directions(q);
        } catch (ExternalServiceUnavailableException e) {
            log.warn("directions query failed ({} {}): {}", q.priority(), q.avoid(), e.getMessage());
            return List.of();
        }
    }
}
