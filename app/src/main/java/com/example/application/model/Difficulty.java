package com.example.application.model;

/** 연습 난이도. 경로 추천 시 도로 복잡도·교통 환경 판단 기준. */
public enum Difficulty implements Labeled {
    EASY("쉬움"), NORMAL("보통"), CHALLENGE("도전");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public Difficulty easier() {
        return this == CHALLENGE ? NORMAL : EASY;
    }
}
