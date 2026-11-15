package com.example.application.domain;

import com.example.application.model.GuidanceLevel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 주행 중 초보자용 음성 안내 규칙. 내비게이션이 알려 주는 다음 회전·주의 지점을 받아
 * 사용자가 고른 안내 수준(최소·표준·적극)에 맞춰 말할 문장을 정한다.
 * 내비의 기본 회전 안내를 반복하지 않고, 초보자에게 필요한 요령과 주의점만 덧붙인다.
 * 같은 지점은 한 번만 안내한다(중복 방지). Android·SDK에 의존하지 않아 단위 테스트할 수 있다.
 */
public final class DriveGuide {

    public enum Maneuver { LEFT, RIGHT, U_TURN, KEEP_LEFT, KEEP_RIGHT, MERGE, EXIT, ROUNDABOUT, STRAIGHT, ARRIVE, OTHER }

    public enum Caution { SCHOOL_ZONE, SPEED_BUMP, ACCIDENT_PRONE, NARROW_ROAD, SLIPPERY, DOWNHILL, RAILROAD, OTHER }

    /** 다음 회전(내비 TBT 첫 항목). */
    public static final class Upcoming {
        public final Maneuver maneuver;
        public final double distanceM;
        public final String description;

        public Upcoming(Maneuver maneuver, double distanceM, String description) {
            this.maneuver = maneuver;
            this.distanceM = distanceM;
            this.description = description == null ? "" : description;
        }
    }

    /** 경로상 주의 지점. */
    public static final class CautionPoint {
        public final String id;
        public final Caution type;
        public final int remainM;

        public CautionPoint(String id, Caution type, int remainM) {
            this.id = id;
            this.type = type;
            this.remainM = remainM;
        }
    }

    private final GuidanceLevel level;
    private final Set<String> spoken = new HashSet<>();

    public DriveGuide(GuidanceLevel level) {
        this.level = level == null ? GuidanceLevel.STANDARD : level;
    }

    public GuidanceLevel level() {
        return level;
    }

    /** 안내를 시작하는 거리(m). 적극 단계는 더 일찍 알려 준다. */
    int leadDistanceM() {
        return level == GuidanceLevel.ACTIVE ? 500 : 300;
    }

    /** 다음 회전에 대해 지금 말할 문장. 말할 것이 없으면 null. */
    public String onUpcoming(List<Upcoming> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        Upcoming next = list.get(0);
        if (next.distanceM > leadDistanceM() || next.distanceM < 30 || !covers(next.maneuver)) {
            return null;
        }
        String key = "turn:" + next.maneuver + ":" + next.description;
        if (!spoken.add(key)) {
            return null;
        }
        return turnMessage(next.maneuver);
    }

    /** 주의 지점에 대해 지금 말할 문장. 가장 가까운 한 곳만 안내한다. */
    public String onCautions(List<CautionPoint> list) {
        if (list == null) {
            return null;
        }
        CautionPoint nearest = null;
        for (CautionPoint c : list) {
            if (c.remainM >= 0 && c.remainM <= leadDistanceM() && covers(c.type)
                    && !spoken.contains("caution:" + c.id)
                    && (nearest == null || c.remainM < nearest.remainM)) {
                nearest = c;
            }
        }
        if (nearest == null) {
            return null;
        }
        spoken.add("caution:" + nearest.id);
        return cautionMessage(nearest.type);
    }

    public String onArrived() {
        if (!spoken.add("arrived")) {
            return null;
        }
        return level == GuidanceLevel.MINIMAL ? "목적지에 도착했어요."
                : "목적지에 도착했어요. 오늘도 안전하게 잘 오셨어요. 정차한 뒤 주행 마치기를 눌러 주세요.";
    }

    /** 단계별로 안내하는 회전 종류. 최소는 초보자가 특히 어려워하는 것만. */
    boolean covers(Maneuver m) {
        switch (m) {
            case U_TURN:
            case MERGE:
            case ROUNDABOUT:
                return true;
            case LEFT:
            case EXIT:
            case KEEP_LEFT:
            case KEEP_RIGHT:
                return level != GuidanceLevel.MINIMAL;
            case RIGHT:
                return level == GuidanceLevel.ACTIVE;
            default:
                return false;
        }
    }

    boolean covers(Caution c) {
        switch (c) {
            case SCHOOL_ZONE:
            case ACCIDENT_PRONE:
            case RAILROAD:
                return true;
            case SPEED_BUMP:
            case NARROW_ROAD:
            case SLIPPERY:
            case DOWNHILL:
                return level != GuidanceLevel.MINIMAL;
            default:
                return false;
        }
    }

    /** 화면 표시용 짧은 이름(주행 중 화면 큰 글씨). */
    public static String label(Maneuver m) {
        switch (m) {
            case LEFT:
                return "좌회전이에요";
            case RIGHT:
                return "우회전이에요";
            case U_TURN:
                return "유턴이에요";
            case KEEP_LEFT:
                return "왼쪽 방향이에요";
            case KEEP_RIGHT:
                return "오른쪽 방향이에요";
            case MERGE:
                return "합류 구간이에요";
            case EXIT:
                return "빠져나가는 길이에요";
            case ROUNDABOUT:
                return "회전교차로예요";
            case ARRIVE:
                return "곧 도착해요";
            default:
                return "직진이에요";
        }
    }

    String turnMessage(Maneuver m) {
        boolean active = level == GuidanceLevel.ACTIVE;
        switch (m) {
            case LEFT:
                return active ? "곧 좌회전이에요. 방향지시등을 미리 켜고, 신호와 맞은편 차를 확인한 뒤 천천히 돌아요."
                        : "곧 좌회전이에요. 신호와 맞은편 차를 확인하고 천천히 돌아요.";
            case RIGHT:
                return "곧 우회전이에요. 방향지시등을 켜고 오른쪽 사이드미러로 자전거와 보행자를 확인해요.";
            case U_TURN:
                if (level == GuidanceLevel.MINIMAL) {
                    return "곧 유턴이에요. 유턴 표지와 신호를 확인해요.";
                }
                return active ? "곧 유턴이에요. 1차로에서 유턴 표지와 신호를 확인하고, 맞은편 차가 끊길 때 핸들을 끝까지 돌려 크게 돌아요."
                        : "곧 유턴이에요. 유턴 표지와 신호를 확인하고 맞은편 차가 끊길 때 돌아요.";
            case MERGE:
                if (level == GuidanceLevel.MINIMAL) {
                    return "곧 합류 구간이에요.";
                }
                return active ? "곧 합류 구간이에요. 방향지시등을 켜고, 사이드미러와 어깨 너머로 뒤차를 확인하며 본선 속도에 맞춰 들어가요."
                        : "곧 합류 구간이에요. 본선 차와 속도를 맞춰 들어가요.";
            case EXIT:
                return active ? "곧 빠져나가는 길이에요. 미리 방향지시등을 켜고 바깥 차로로 옮겨 두세요."
                        : "곧 빠져나가는 길이에요. 미리 바깥 차로로 옮겨요.";
            case KEEP_LEFT:
                return active ? "곧 왼쪽 방향이에요. 바닥 화살표를 보고 왼쪽 차로로 미리 맞춰요."
                        : "곧 왼쪽 방향이에요. 차로를 미리 맞춰요.";
            case KEEP_RIGHT:
                return active ? "곧 오른쪽 방향이에요. 바닥 화살표를 보고 오른쪽 차로로 미리 맞춰요."
                        : "곧 오른쪽 방향이에요. 차로를 미리 맞춰요.";
            case ROUNDABOUT:
                if (level == GuidanceLevel.MINIMAL) {
                    return "곧 회전교차로예요.";
                }
                return "곧 회전교차로예요. 안에 있는 차가 먼저예요. 속도를 줄이고 천천히 들어가요.";
            default:
                return null;
        }
    }

    String cautionMessage(Caution c) {
        boolean minimal = level == GuidanceLevel.MINIMAL;
        switch (c) {
            case SCHOOL_ZONE:
                return minimal ? "어린이보호구역이에요." : "어린이보호구역이에요. 시속 30킬로미터 이하로 천천히 가요.";
            case ACCIDENT_PRONE:
                return minimal ? "사고 다발 구간이에요." : "사고가 잦은 구간이에요. 앞차와 거리를 넉넉히 두세요.";
            case RAILROAD:
                return "철길 건널목이에요. 일단 멈추고 좌우를 확인해요.";
            case SPEED_BUMP:
                return "과속방지턱이 있어요. 미리 속도를 줄여요.";
            case NARROW_ROAD:
                return "길이 좁아져요. 속도를 줄이고 차로 가운데를 유지해요.";
            case SLIPPERY:
                return "미끄러운 길이에요. 급하게 브레이크를 밟지 말고 천천히 가요.";
            case DOWNHILL:
                return "내리막길이에요. 브레이크를 나눠 밟으며 속도를 줄여요.";
            default:
                return null;
        }
    }
}
