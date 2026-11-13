package com.example.application.model;

/** 출발 전 사전 브리핑할 복잡 구간 한 곳. */
public class BriefingPoint {
    public enum Type {
        COMPLEX_INTERSECTION("복잡한 교차로"),
        MERGE("합류 구간"),
        LANE_CHANGE_PREP("차선 변경 준비"),
        U_TURN("유턴 지점"),
        LEFT_TURN("비보호 좌회전"),
        PARKING("주차 구간");

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }

    public int order;
    public Type type;
    /** 출발지로부터 거리(km). */
    public double distanceKm;
    /** 짧은 통과 방법(목록 표시용). 예: "직진 차로를 유지해요" */
    public String instruction;
    /** 음성 브리핑 문장(상세). */
    public String speech;
    public GeoPoint point;
}
