package com.example.application.model;

/** 전체 안내량(온보딩 0-4 "얼마나 자세히 안내해 드릴까요?"). 설정에서 언제든 변경 가능. */
public enum GuidanceLevel implements Labeled {
    MINIMAL("최소", "꼭 필요한 안내만 짧게 드려요.",
            "\"300미터 앞 우회전이에요.\""),
    STANDARD("표준", "주의할 점을 한 번씩 알려드려요.",
            "\"300미터 앞 우회전이에요. 미리 오른쪽 차로로 옮겨 주세요.\""),
    ACTIVE("적극", "조작 방법과 주의할 점까지 자세히 알려드려요.",
            "\"300미터 앞 우회전이에요. 방향지시등을 먼저 켜고, 오른쪽 사이드미러로 뒤차를 확인한 뒤 차로를 옮겨 주세요.\"");

    private final String label;
    private final String description;
    public final String example;

    GuidanceLevel(String label, String description, String example) {
        this.label = label;
        this.description = description;
        this.example = example;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String description() {
        return description;
    }

    public GuidanceLevel raised() {
        return this == MINIMAL ? STANDARD : ACTIVE;
    }
}
