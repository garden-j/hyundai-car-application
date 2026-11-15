package com.example.application.model;

/** 주요 운전 목적(운전 경험 입력). */
public enum DrivingPurpose implements Labeled {
    COMMUTE("출퇴근"),
    SCHOOL("통학·등하원"),
    SHOPPING("장보기·볼일"),
    TRAVEL("여행·나들이"),
    PRACTICE_ONLY("연습만 해요");

    private final String label;

    DrivingPurpose(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
