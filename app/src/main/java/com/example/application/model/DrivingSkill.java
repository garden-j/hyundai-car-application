package com.example.application.model;

/** 연습할 주행 기술. */
public enum DrivingSkill implements Labeled {
    LEFT_TURN("좌회전"),
    RIGHT_TURN("우회전"),
    U_TURN("유턴"),
    LANE_CHANGE("차로 변경"),
    INTERSECTION("교차로 통과"),
    MERGE("합류"),
    PARKING("주차");

    private final String label;

    DrivingSkill(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
