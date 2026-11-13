package com.example.application.domain;

import com.example.application.model.Difficulty;
import com.example.application.model.DrivingSkill;
import com.example.application.model.Familiarity;
import com.example.application.model.GeoPoint;
import com.example.application.model.Place;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RoadEnvironment;
import com.example.application.model.RouteCandidate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 연습 경로 후보 3개를 만든다(백엔드 경로 추천 API가 준비되기 전의 기기 내 버전).
 * <ul>
 *   <li>목적지가 있으면(A) 출발지 → 목적지(왕복이면 다시 출발지) 경로 3종</li>
 *   <li>목적지가 없으면(B) 출발지에서 돌아오는 연습 코스 3종</li>
 * </ul>
 * 경로 요소 개수는 거리·난이도·연습 목표로 추정하며, 실제 도로 데이터는 백엔드 연동 시 대체된다.
 * 같은 입력이면 항상 같은 결과를 낸다(테스트·데모 재현성).
 */
public final class RouteCandidateGenerator {
    /** 직선 거리 대비 실제 도로 거리 보정 계수. */
    static final double ROAD_FACTOR = 1.3;

    private RouteCandidateGenerator() {
    }

    public static List<RouteCandidate> generate(GeoPoint origin, PracticeSetup s) {
        boolean hasDestination = s.hasDestination();
        double baseKm = baseDistanceKm(origin, s, hasDestination);
        Difficulty difficulty = s.effectiveDifficulty();

        RouteCandidate wide = build(RouteCandidate.Kind.WIDE_EASY, origin, s, baseKm * 1.25, difficulty, hasDestination);
        RouteCandidate shortest = build(RouteCandidate.Kind.SHORTEST, origin, s, baseKm, difficulty, hasDestination);
        RouteCandidate goal = build(RouteCandidate.Kind.GOAL_FOCUSED, origin, s, baseKm * 1.15, difficulty, hasDestination);

        List<RouteCandidate> list = difficulty == Difficulty.CHALLENGE && !s.isHighTension()
                ? new ArrayList<>(Arrays.asList(goal, shortest, wide))
                : new ArrayList<>(Arrays.asList(wide, shortest, goal));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).id = "r" + (i + 1);
        }
        return list;
    }

    static double baseDistanceKm(GeoPoint origin, PracticeSetup s, boolean hasDestination) {
        if (hasDestination) {
            double km = origin.distanceKmTo(s.destination.destination.point) * ROAD_FACTOR;
            km = Math.max(km, 1.0);
            return s.destination.roundTrip ? km * 2 : km;
        }
        if (s.amount != null) {
            return Math.max(1.0, s.amount.estimatedKm());
        }
        switch (s.effectiveDifficulty()) {
            case EASY:
                return 4.0;
            case CHALLENGE:
                return 8.0;
            default:
                return 6.0;
        }
    }

    private static double multiplier(Difficulty d) {
        switch (d) {
            case EASY:
                return 0.5;
            case CHALLENGE:
                return 1.6;
            default:
                return 1.0;
        }
    }

    private static RouteCandidate build(RouteCandidate.Kind kind, GeoPoint origin, PracticeSetup s,
                                        double km, Difficulty d, boolean hasDestination) {
        RouteCandidate r = new RouteCandidate();
        r.kind = kind;
        r.origin = origin;
        r.distanceKm = round1(km);

        double m = multiplier(d);
        if (s.familiarity == Familiarity.NEW) {
            m *= 1.15;
        }
        boolean lowTraffic = s.environments.contains(RoadEnvironment.LOW_TRAFFIC);
        if (lowTraffic) {
            m *= 0.8;
        }

        // 기본(최단 경로) 요소 밀도: km당 개수
        double lane = km * 0.6 * m;
        double complex = km * 0.25 * m;
        double merge = km * 0.08 * m;
        double left = km * 0.4 * m;
        double uturn = km * 0.05 * m;

        switch (kind) {
            case WIDE_EASY:
                r.laneChanges = round(lane * 0.4);
                r.complexIntersections = round(complex * 0.3);
                r.merges = 0;
                r.leftTurns = round(left * 0.5);
                r.uTurns = 0;
                r.title = "넓은 길로 가요";
                r.reason = r.laneChanges <= 2 ? "차선 변경이 적고 도로가 넓어요" : "도로가 넓고 복잡한 교차로가 적어요";
                r.minutes = minutes(km, 24);
                break;
            case SHORTEST:
                r.laneChanges = round(lane);
                r.complexIntersections = round(complex);
                r.merges = round(merge);
                r.leftTurns = round(left);
                r.uTurns = round(uturn);
                r.title = "최단 거리예요";
                r.reason = r.complexIntersections > 0
                        ? "교차로가 " + r.complexIntersections + "개 있어요"
                        : "가장 짧은 길이에요";
                r.minutes = minutes(km, 26);
                break;
            case GOAL_FOCUSED:
            default:
                r.laneChanges = round(lane * 0.8);
                r.complexIntersections = round(complex * 0.8);
                r.merges = round(merge * 0.5);
                r.leftTurns = round(left * 0.8);
                r.uTurns = 0;
                applyGoalFocus(r, s, d);
                r.minutes = minutes(km, 22) + r.uTurns + r.parkingSpots * 2;
                break;
        }
        RouteDifficultyScorer.score(r);
        assignGeometry(r, origin, s, km, hasDestination);
        return r;
    }

    /** 선택한 기술을 난이도에 맞는 횟수만큼 경험하도록 요소를 채운다. */
    private static void applyGoalFocus(RouteCandidate r, PracticeSetup s, Difficulty d) {
        int reps = d == Difficulty.EASY ? 1 : d == Difficulty.NORMAL ? 2 : 3;
        r.title = "목표 연습 최적화";
        DrivingSkill main = null;
        for (DrivingSkill skill : s.skills) {
            if (main == null) {
                main = skill;
            }
            switch (skill) {
                case LEFT_TURN:
                    r.leftTurns = Math.max(r.leftTurns, reps + 1);
                    break;
                case RIGHT_TURN:
                    break; // 우회전은 점수 가중치 없음(부담 낮음). 이유 문장에만 반영
                case U_TURN:
                    r.uTurns = Math.max(r.uTurns, reps);
                    break;
                case LANE_CHANGE:
                    r.laneChanges = Math.max(r.laneChanges, reps + 2);
                    break;
                case INTERSECTION:
                    r.complexIntersections = Math.max(r.complexIntersections, reps);
                    break;
                case MERGE:
                    r.merges = Math.max(r.merges, reps);
                    break;
                case PARKING:
                    r.parkingSpots = Math.max(r.parkingSpots, 1);
                    break;
            }
        }
        if (main != null) {
            r.reason = skillReason(main, r, reps);
            return;
        }
        if (s.environments.contains(RoadEnvironment.COMPLEX_INTERSECTION)) {
            r.complexIntersections = Math.max(r.complexIntersections, reps);
            r.reason = "복잡한 교차로를 " + r.complexIntersections + "번 지나요";
        } else if (s.environments.contains(RoadEnvironment.RESIDENTIAL)) {
            r.reason = "주택가 좁은 길을 지나요";
        } else if (s.environments.contains(RoadEnvironment.WIDE_MULTILANE)) {
            r.laneChanges = Math.max(r.laneChanges, reps + 1);
            r.reason = "다차선 도로를 달려요";
        } else {
            r.title = "한산한 길 위주";
            r.reason = "차가 적은 시간대 도로 위주예요";
        }
    }

    private static String skillReason(DrivingSkill skill, RouteCandidate r, int reps) {
        switch (skill) {
            case LEFT_TURN:
                return "좌회전 지점이 " + r.leftTurns + "곳 있어요";
            case RIGHT_TURN:
                return "우회전 지점이 " + (reps + 1) + "곳 있어요";
            case U_TURN:
                return "유턴 지점이 " + r.uTurns + "곳 있어요";
            case LANE_CHANGE:
                return "차로 변경을 " + r.laneChanges + "번 해 봐요";
            case INTERSECTION:
                return "교차로를 " + r.complexIntersections + "번 지나요";
            case MERGE:
                return "합류 구간이 " + r.merges + "곳 있어요";
            case PARKING:
            default:
                return "도착지에서 주차를 연습해요";
        }
    }

    /** 경유지·도착지·미리보기 선을 만든다. 후보마다 방향을 달리해 서로 다른 길이 되게 한다. */
    private static void assignGeometry(RouteCandidate r, GeoPoint origin, PracticeSetup s,
                                       double km, boolean hasDestination) {
        Place originPlace = new Place("출발지", "", origin);
        double bearing = r.kind == RouteCandidate.Kind.WIDE_EASY ? 30
                : r.kind == RouteCandidate.Kind.SHORTEST ? 150 : 270;
        r.previewPath.add(origin);
        if (hasDestination) {
            Place dest = s.destination.destination;
            if (s.destination.roundTrip) {
                r.waypoints.add(dest);
                r.destination = originPlace;
                r.previewPath.add(dest.point);
                r.previewPath.add(origin);
            } else {
                if (r.kind != RouteCandidate.Kind.SHORTEST) {
                    GeoPoint mid = midpoint(origin, dest.point);
                    double detour = Math.max(0.3, origin.distanceKmTo(dest.point) * 0.15);
                    GeoPoint via = mid.offsetKm(detour * Math.cos(Math.toRadians(bearing)),
                            detour * Math.sin(Math.toRadians(bearing)));
                    r.waypoints.add(new Place("경유지", "", via));
                    r.previewPath.add(via);
                }
                r.destination = dest;
                r.previewPath.add(dest.point);
            }
            return;
        }
        // 연습 코스: 출발지 → 경유1 → 경유2 → 출발지 (삼각형 한 바퀴)
        double side = km / 3.0;
        GeoPoint p1 = origin.offsetKm(side * Math.cos(Math.toRadians(bearing)),
                side * Math.sin(Math.toRadians(bearing)));
        GeoPoint p2 = origin.offsetKm(side * Math.cos(Math.toRadians(bearing + 60)),
                side * Math.sin(Math.toRadians(bearing + 60)));
        r.waypoints.add(new Place("연습 코스 지점 1", "", p1));
        r.waypoints.add(new Place("연습 코스 지점 2", "", p2));
        r.destination = originPlace;
        r.previewPath.add(p1);
        r.previewPath.add(p2);
        r.previewPath.add(origin);
    }

    private static GeoPoint midpoint(GeoPoint a, GeoPoint b) {
        return new GeoPoint((a.latitude + b.latitude) / 2, (a.longitude + b.longitude) / 2);
    }

    private static int round(double v) {
        return (int) Math.max(0, Math.round(v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static int minutes(double km, double kmh) {
        return Math.max(1, (int) Math.round(km / kmh * 60));
    }

    public static String formatDistance(double km) {
        return String.format(Locale.KOREA, "%.1f km", km);
    }
}
