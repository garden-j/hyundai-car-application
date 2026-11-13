package com.example.application.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 운전 경험 프로필. 응답하지 않은 항목은 null(미입력)로 저장한다.
 * 안내량 설정, 연습 목표·경로 추천, 오늘의 추천에 사용한다.
 */
public class DriverProfile {
    public LicensePeriod licensePeriod;
    /** 운전 연수(도로 연수)를 받아 본 적이 있는지. */
    public Boolean hadDrivingLessons;
    /** 운전 빈도 1(가끔 함) ~ 5(자주 함). */
    public Integer drivingFrequency;
    public AverageDriveTime averageDriveTime;
    public Set<DifficultSituation> difficulties = EnumSet.noneOf(DifficultSituation.class);
    public GuidanceLevel guidanceLevel;
    public boolean onboardingCompleted;

    public GuidanceLevel guidanceLevelOrDefault() {
        return guidanceLevel != null ? guidanceLevel : GuidanceLevel.STANDARD;
    }

    /** 초보 정도 추정: 면허 6개월 미만이거나 운전 빈도가 낮으면 true. */
    public boolean isNovice() {
        boolean recentLicense = licensePeriod == null
                || licensePeriod == LicensePeriod.UNDER_1_MONTH
                || licensePeriod == LicensePeriod.UNDER_6_MONTHS;
        boolean rarely = drivingFrequency == null || drivingFrequency <= 2;
        return recentLicense || rarely;
    }
}
