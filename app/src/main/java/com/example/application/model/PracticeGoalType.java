package com.example.application.model;

/** 연습 목표 유형(복수 선택). 선택한 유형만 세부 설정 화면을 거친다. */
public enum PracticeGoalType implements Labeled {
    SKILL("주행 기술", "좌회전, 유턴, 주차처럼\n한 가지를 집중해서"),
    DISTANCE_TIME("거리, 시간", "원하는 거리나 시간만큼\n꾸준히 달려요"),
    DESTINATION("목적지 기반", "실제로 가고 싶은\n곳까지 달려요"),
    ENVIRONMENT("주행 환경", "좁은 길, 야간처럼\n다양한 환경에서 익숙해져요");

    private final String label;
    private final String description;

    PracticeGoalType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String description() {
        return description;
    }
}
