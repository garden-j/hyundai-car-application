package com.example.application.model;

/** 연습 지역 익숙함. */
public enum Familiarity implements Labeled {
    FAMILIAR("익숙한 곳"), NEW("새로운 곳");

    private final String label;

    Familiarity(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
