package com.example.application.model;

/** 면허 취득 후 경과 기간. */
public enum LicensePeriod implements Labeled {
    UNDER_1_MONTH("1개월 미만"),
    UNDER_6_MONTHS("6개월 미만"),
    UNDER_1_YEAR("1년 미만"),
    ONE_TO_3_YEARS("1~3년"),
    OVER_3_YEARS("3년 이상");

    private final String label;

    LicensePeriod(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
