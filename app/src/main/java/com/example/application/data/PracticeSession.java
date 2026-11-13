package com.example.application.data;

import com.example.application.core.pleos.vehicle.VehicleSettings;
import com.example.application.model.PracticePreset;

/**
 * 진행 중인 연습 세션 기록. 프리셋 적용 직전 차량 설정을 저장해 두고,
 * 연습 종료 후 "연습 전·현재 설정 비교 → 복원"에 사용한다.
 */
public class PracticeSession {
    public long startedAtMillis;
    public String routeTitle;
    public double routeDistanceKm;
    public int routeMinutes;
    public PracticePreset preset;
    /** 프리셋 적용 전 차량 설정. */
    public VehicleSettings settingsBefore;
    /** 앱이 실제로 적용에 성공한 값(사용자가 이후 직접 바꿨는지 비교용). */
    public VehicleSettings settingsApplied;
}
