package com.example.application.model;

/** 거리·시간 달성형 목표. 거리(km) 또는 시간(분) 중 하나만 정한다. */
public class GoalAmount {
    public enum Unit {
        DISTANCE_KM("km", 1, 1, 50), TIME_MIN("분", 5, 5, 120);

        public final String suffix;
        /** 직접 입력 화면의 +/- 한 번에 바뀌는 양. */
        public final int step;
        public final int min;
        public final int max;

        Unit(String suffix, int step, int min, int max) {
            this.suffix = suffix;
            this.step = step;
            this.min = min;
            this.max = max;
        }
    }

    public Unit unit;
    public int value;

    public GoalAmount() {
    }

    public GoalAmount(Unit unit, int value) {
        this.unit = unit;
        this.value = value;
    }

    /** 도심 평균 25km/h 기준 예상 주행 거리(km). */
    public double estimatedKm() {
        return unit == Unit.DISTANCE_KM ? value : value * 25.0 / 60.0;
    }

    /** 예상 주행 시간(분). */
    public int estimatedMinutes() {
        return unit == Unit.TIME_MIN ? value : (int) Math.round(value * 60.0 / 25.0);
    }

    public String display() {
        return value + unit.suffix;
    }
}
