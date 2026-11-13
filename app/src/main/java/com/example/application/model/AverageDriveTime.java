package com.example.application.model;

/** 1회 평균 운전 시간. */
public enum AverageDriveTime implements Labeled {
    UNDER_30_MIN("30분 이내"),
    UNDER_1_HOUR("30분~1시간"),
    OVER_1_HOUR("1시간 이상");

    private final String label;

    AverageDriveTime(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
