package com.example.application.data.direct.routing;

import com.example.application.data.direct.common.Lists;

import java.util.Set;

/**
 * 카카오모빌리티 안내 타입(guide.type) 분류. 코드표: developers.kakaomobility.com/guide/navi-api/reference
 * 초보 운전자 부담 관점으로 묶는다.
 */
public final class GuideTypes {
    private GuideTypes() {
    }

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int U_TURN = 3;
    public static final int ORIGIN = 100;
    public static final int DESTINATION = 101;
    public static final int WAYPOINT = 1000;

    /** 고속도로·도시고속도로 진입, 분기 후 합류: 본선 합류가 필요한 지점. */
    public static final Set<Integer> MERGE = Lists.setOf(10, 11, 12, 45, 46, 47, 48, 49, 86);

    /** 방향 유지·출구: 미리 차로를 맞춰야 하는 지점. */
    public static final Set<Integer> LANE_POSITIONING = Lists.setOf(5, 6, 7, 8, 9, 42, 43, 44, 82, 83);

    /** n시 방향 갈림길·로터리·회전교차로: 복잡한 교차로. */
    public static boolean isComplexIntersection(int type) {
        return (type >= 18 && type <= 41) || (type >= 70 && type <= 81);
    }

    public static boolean isMerge(int type) {
        return MERGE.contains(type);
    }

    public static boolean isLanePositioning(int type) {
        return LANE_POSITIONING.contains(type);
    }
}
