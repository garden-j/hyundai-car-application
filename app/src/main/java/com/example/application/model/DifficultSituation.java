package com.example.application.model;

/** 평소 운전할 때 어려웠던 상황(온보딩 0-3, 복수 선택). */
public enum DifficultSituation implements Labeled {
    VEHICLE_CONTROL("차량 조작", null),
    LANE_CHANGE("차로 변경", DrivingSkill.LANE_CHANGE),
    MERGE("합류", DrivingSkill.MERGE),
    ROUND_TRIP("왕복", null),
    INTERSECTION_TURN("교차로 회전", DrivingSkill.INTERSECTION),
    UNEXPECTED("돌발 상황", null),
    PARKING("주차", DrivingSkill.PARKING),
    U_TURN("U턴", DrivingSkill.U_TURN);

    private final String label;
    /** 연습 기술과 연결되는 상황이면 해당 기술(오늘의 추천에 사용). */
    public final DrivingSkill relatedSkill;

    DifficultSituation(String label, DrivingSkill relatedSkill) {
        this.label = label;
        this.relatedSkill = relatedSkill;
    }

    @Override
    public String label() {
        return label;
    }
}
