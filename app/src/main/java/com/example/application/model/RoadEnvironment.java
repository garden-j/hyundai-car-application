package com.example.application.model;

/** 익숙해지고 싶은 주행 환경. 화면에서는 그룹별로 나눠 보여준다. */
public enum RoadEnvironment implements Labeled {
    RESIDENTIAL(Group.ROAD, "주택가 · 좁은 도로"),
    WIDE_MULTILANE(Group.ROAD, "넓은 다차선 도로"),
    LOW_TRAFFIC(Group.TRAFFIC, "교통량이 적은 도로"),
    COMPLEX_INTERSECTION(Group.TRAFFIC, "복잡한 교차로"),
    NIGHT(Group.TIME_WEATHER, "야간 주행"),
    RAIN(Group.TIME_WEATHER, "우천 시 주행");

    public enum Group {
        ROAD("도로 유형"), TRAFFIC("교통 환경"), TIME_WEATHER("시간 · 날씨");

        public final String label;

        Group(String label) {
            this.label = label;
        }
    }

    public final Group group;
    private final String label;

    RoadEnvironment(Group group, String label) {
        this.group = group;
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
