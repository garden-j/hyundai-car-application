package com.example.application.domain;

import com.example.application.model.DriverProfile;
import com.example.application.model.LicensePeriod;
import com.example.application.model.PracticePreset;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RoadEnvironment;

/**
 * 연습 목표·날씨·예상 주행 시간으로 연습 모드 프리셋을 추천한다.
 * 우선순위: 악천후(시야·안전) > 조작 연습 > 장거리 > 집중 주행(기본).
 */
public final class PresetRecommender {
    public static final int LONG_DRIVE_MINUTES = 30;
    public static final float COLD_C = 5f;
    public static final float HOT_C = 28f;

    public static final class Recommendation {
        public final PracticePreset preset;
        public final String reason;

        Recommendation(PracticePreset preset, String reason) {
            this.preset = preset;
            this.reason = reason;
        }
    }

    private PresetRecommender() {
    }

    /**
     * @param outsideTempC 바깥 기온(모르면 null)
     * @param raining      비 오는 중(모르면 null). 와이퍼 작동 여부나 기상 정보로 판단
     */
    public static Recommendation recommend(PracticeSetup s, DriverProfile profile,
                                           Float outsideTempC, Boolean raining) {
        boolean rainGoal = s.environments.contains(RoadEnvironment.RAIN);
        boolean cold = outsideTempC != null && outsideTempC <= COLD_C;
        boolean hot = outsideTempC != null && outsideTempC >= HOT_C;
        if (rainGoal || Boolean.TRUE.equals(raining) || cold || hot) {
            String why = rainGoal || Boolean.TRUE.equals(raining) ? "비 오는 날 시야를 확보하도록"
                    : cold ? "추운 날씨에 맞춰" : "더운 날씨에 맞춰";
            return new Recommendation(PracticePreset.WEATHER, why + " " + Josa.eulReul(PracticePreset.WEATHER.label()) + " 추천해요.");
        }
        if (!s.skills.isEmpty()) {
            return new Recommendation(PracticePreset.MANEUVER, Josa.eulReul(PracticePreset.MANEUVER.label()) + " 추천해요.");
        }
        int minutes = estimatedMinutes(s);
        if (minutes >= LONG_DRIVE_MINUTES) {
            return new Recommendation(PracticePreset.LONG_DRIVE,
                    "약 " + minutes + "분 달리니 " + Josa.eulReul(PracticePreset.LONG_DRIVE.label()) + " 추천해요.");
        }
        boolean firstDrive = profile != null && profile.licensePeriod == LicensePeriod.UNDER_1_MONTH;
        return new Recommendation(PracticePreset.FOCUS,
                (firstDrive ? "첫 운전이니 " : "") + Josa.eulReul(PracticePreset.FOCUS.label()) + " 추천해요.");
    }

    static int estimatedMinutes(PracticeSetup s) {
        if (s.selectedRoute != null) {
            return s.selectedRoute.minutes;
        }
        if (s.amount != null) {
            return s.amount.estimatedMinutes();
        }
        return 0;
    }
}
