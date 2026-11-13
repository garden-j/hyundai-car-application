package com.example.application.model;

/** 긴장하는 이유(복수 선택). 경로 추천·안내 수준·Relax 안내에 반영한다. */
public enum TensionReason implements Labeled {
    UNFAMILIAR_ROAD("처음 가는 길이라서"),
    HEAVY_TRAFFIC("차가 많을 것 같아서"),
    WEATHER_DARK("날씨나 어두움 때문에"),
    SKILL_WORRY("조작이 아직 서툴러서"),
    PAST_EXPERIENCE("지난 운전이 힘들어서"),
    CONDITION("컨디션이 좋지 않아서");

    private final String label;

    TensionReason(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
