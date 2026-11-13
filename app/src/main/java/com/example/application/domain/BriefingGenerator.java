package com.example.application.domain;

import com.example.application.model.BriefingPoint;
import com.example.application.model.BriefingPoint.Type;
import com.example.application.model.GeoPoint;
import com.example.application.model.RouteCandidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 선택한 경로의 복잡 구간을 주행 순서대로 정리해 사전 브리핑을 만든다
 * (기능 명세 "복잡한 구간 사전 브리핑"). 백엔드 구간 분석 API 연동 전 기기 내 버전.
 */
public final class BriefingGenerator {
    /** 너무 길면 기억하기 어려워 최대 개수를 둔다. */
    public static final int MAX_POINTS = 5;

    private BriefingGenerator() {
    }

    public static List<BriefingPoint> generate(RouteCandidate route) {
        List<Type> events = new ArrayList<>();
        // 부담이 큰 구간을 먼저 담고, 최대 개수를 넘으면 뒤쪽(덜 중요한 것)을 버린다.
        repeat(events, Type.COMPLEX_INTERSECTION, route.complexIntersections);
        repeat(events, Type.MERGE, route.merges);
        repeat(events, Type.U_TURN, route.uTurns);
        repeat(events, Type.LANE_CHANGE_PREP, Math.min(route.laneChanges, 2));
        repeat(events, Type.LEFT_TURN, Math.min(route.leftTurns, 2));
        if (events.size() > MAX_POINTS) {
            events = new ArrayList<>(events.subList(0, MAX_POINTS));
        }
        boolean parking = route.parkingSpots > 0;
        int total = events.size() + (parking ? 1 : 0);

        List<BriefingPoint> points = new ArrayList<>();
        int[] seen = new int[Type.values().length];
        for (int i = 0; i < events.size(); i++) {
            Type t = events.get(i);
            // 경로 위에 고르게 배치하되 유형별로 섞이도록 순서를 엇갈린다.
            double fraction = (interleave(i, events.size()) + 1.0) / (total + 1.0);
            points.add(point(t, seen[t.ordinal()]++, fraction, route));
        }
        if (parking) {
            points.add(point(Type.PARKING, 0, 0.97, route));
        }
        points.sort(Comparator.comparingDouble(p -> p.distanceKm));
        for (int i = 0; i < points.size(); i++) {
            points.get(i).order = i + 1;
        }
        return points;
    }

    /** 브리핑 전체를 한 번에 읽을 문장 목록(인트로 + 구간별 + 마무리). */
    public static List<String> speechScript(List<BriefingPoint> points) {
        List<String> lines = new ArrayList<>();
        if (points.isEmpty()) {
            lines.add("이번 경로에는 특별히 복잡한 구간이 없어요. 편하게 출발해요.");
            return lines;
        }
        lines.add("이번 경로에서 주의할 곳은 " + points.size() + "곳이에요. 순서대로 알려드릴게요.");
        for (BriefingPoint p : points) {
            lines.add(p.speech);
        }
        lines.add("주행 중에도 해당 구간 앞에서 다시 알려드릴게요.");
        return lines;
    }

    private static void repeat(List<Type> list, Type t, int n) {
        for (int i = 0; i < n; i++) {
            list.add(t);
        }
    }

    /** 0,1,2,3,4 → 0,2,4,1,3 처럼 섞어 같은 유형이 몰리지 않게 한다. */
    private static int interleave(int i, int n) {
        int half = (n + 1) / 2;
        return i < half ? i * 2 : (i - half) * 2 + 1;
    }

    private static BriefingPoint point(Type type, int occurrence, double fraction, RouteCandidate route) {
        BriefingPoint p = new BriefingPoint();
        p.type = type;
        p.distanceKm = Math.round(route.distanceKm * fraction * 10.0) / 10.0;
        p.point = along(route.previewPath, fraction);
        String[] text = texts(type, occurrence);
        p.instruction = text[0];
        p.speech = spokenDistance(p.distanceKm) + " 앞 " + Josa.ieyo(type.label) + ". " + text[1];
        return p;
    }

    private static String[] texts(Type type, int occurrence) {
        switch (type) {
            case COMPLEX_INTERSECTION:
                return occurrence % 2 == 0
                        ? new String[]{"직진 차로를 유지해요",
                        "차로가 여러 갈래로 나뉘어요. 바닥의 화살표를 보고 직진 차로를 유지하세요."}
                        : new String[]{"신호를 보고 천천히 통과해요",
                        "신호가 바뀌는 중이면 무리하지 말고 정지선 앞에서 기다리세요."};
            case MERGE:
                return new String[]{"미리 속도를 조절해요",
                        "합류 차로가 끝나기 전에 본선 차량 흐름을 확인하고, 사이드미러로 간격을 본 뒤 진입하세요."};
            case U_TURN:
                return new String[]{"유턴 신호를 확인해요",
                        "1차로에서 유턴 표지와 신호를 확인하고, 맞은편 차가 끊길 때 크게 돌아 주세요."};
            case LANE_CHANGE_PREP:
                return occurrence % 2 == 0
                        ? new String[]{"오른쪽 차로로 미리 옮겨요",
                        "방향지시등을 먼저 켜고, 오른쪽 사이드미러와 사각지대를 확인한 뒤 차로를 옮기세요."}
                        : new String[]{"왼쪽 차로로 미리 옮겨요",
                        "방향지시등을 먼저 켜고, 왼쪽 사이드미러와 사각지대를 확인한 뒤 차로를 옮기세요."};
            case LEFT_TURN:
                return new String[]{"맞은편 차가 끊길 때 돌아요",
                        "비보호 좌회전이에요. 맞은편 직진 차량이 모두 지나간 뒤 천천히 돌아 주세요."};
            case PARKING:
            default:
                return new String[]{"주차 공간을 미리 살펴요",
                        "도착하면 비어 있는 주차 칸을 먼저 찾고, 비상등을 켠 뒤 천천히 주차하세요."};
        }
    }

    public static String spokenDistance(double km) {
        if (km < 1.0) {
            int m = (int) (Math.round(km * 10) * 100);
            return Math.max(100, m) + "미터";
        }
        return String.format(Locale.KOREA, "%.1f킬로미터", km);
    }

    public static String listDistance(double km) {
        return String.format(Locale.KOREA, "%.1f km 앞", km);
    }

    /** 미리보기 선 위에서 비율(0~1) 위치의 점. */
    static GeoPoint along(List<GeoPoint> path, double fraction) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (path.size() == 1) {
            return path.get(0);
        }
        double total = 0;
        double[] seg = new double[path.size() - 1];
        for (int i = 0; i < seg.length; i++) {
            seg[i] = path.get(i).distanceKmTo(path.get(i + 1));
            total += seg[i];
        }
        double target = total * Math.max(0, Math.min(1, fraction));
        for (int i = 0; i < seg.length; i++) {
            if (target <= seg[i] || i == seg.length - 1) {
                double t = seg[i] == 0 ? 0 : Math.min(1, target / seg[i]);
                GeoPoint a = path.get(i);
                GeoPoint b = path.get(i + 1);
                return new GeoPoint(a.latitude + (b.latitude - a.latitude) * t,
                        a.longitude + (b.longitude - a.longitude) * t);
            }
            target -= seg[i];
        }
        return path.get(path.size() - 1);
    }
}
