package com.example.application.model;

import com.example.application.core.pleos.vehicle.SettingKey;
import com.example.application.model.PresetItem.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 연습 목표·주행 환경별 차량 내부 환경 프리셋 4종(기능 명세 "연습 모드 차량 환경 조절").
 * 차량 API가 지원하는 항목만 실제로 적용하고, 음악은 API가 없어 사용자에게 안내한다.
 */
public enum PracticePreset implements Labeled {
    FOCUS("집중 주행 모드", "첫 운전·기본 주행에 집중할 수 있게 자극을 줄여요.", Arrays.asList(
            PresetItem.manual(Category.MUSIC, "직접 꺼 주세요"),
            PresetItem.control(Category.CLIMATE, "쾌적한 온도 · 약한 풍량", false,
                    SettingKey.HVAC_POWER, true, SettingKey.CABIN_TEMPERATURE, 22.0f, SettingKey.FAN_SPEED, PresetItem.FAN_LOW),
            PresetItem.control(Category.WINDOW, "닫힘", false, SettingKey.DRIVER_WINDOW, 0),
            PresetItem.keep(Category.SEAT, "저장된 설정 유지"),
            PresetItem.control(Category.WHEEL_HEAT, "꺼짐", false, SettingKey.STEERING_WHEEL_HEAT, 0))),

    MANEUVER("조작 연습 모드", "좌회전·유턴·차로 변경·주차 조작과 주변 확인에 집중해요.", Arrays.asList(
            PresetItem.manual(Category.MUSIC, "직접 꺼 주세요"),
            PresetItem.control(Category.CLIMATE, "쾌적한 온도 · 약한 풍량", false,
                    SettingKey.HVAC_POWER, true, SettingKey.CABIN_TEMPERATURE, 22.0f, SettingKey.FAN_SPEED, PresetItem.FAN_LOW),
            PresetItem.control(Category.WINDOW, "닫힘", false, SettingKey.DRIVER_WINDOW, 0),
            PresetItem.keep(Category.SEAT, "저장된 설정 유지"),
            PresetItem.control(Category.WHEEL_HEAT, "꺼짐", false, SettingKey.STEERING_WHEEL_HEAT, 0))),

    WEATHER("악천후·환경 적응 모드", "비·추위·더위 속에서도 시야와 쾌적함을 지켜요.", Arrays.asList(
            PresetItem.manual(Category.MUSIC, "직접 꺼 주세요"),
            PresetItem.control(Category.CLIMATE, "쾌적한 온도 · 보통 풍량", false,
                    SettingKey.HVAC_POWER, true, SettingKey.CABIN_TEMPERATURE, 22.0f, SettingKey.FAN_SPEED, 3),
            PresetItem.control(Category.DEFROST, "앞유리 김서림 제거 켜기", false, SettingKey.DEFROSTER, true),
            PresetItem.control(Category.WINDOW, "닫힘", false, SettingKey.DRIVER_WINDOW, 0),
            PresetItem.keep(Category.SEAT, "저장된 설정 유지"),
            PresetItem.control(Category.WHEEL_HEAT, "켜기 (동의 후)", true, SettingKey.STEERING_WHEEL_HEAT, 1))),

    LONG_DRIVE("장거리·지속 주행 모드", "오래 달려도 편안한 환경을 유지해요.", Arrays.asList(
            PresetItem.keep(Category.MUSIC, "원하는 음량 유지"),
            PresetItem.control(Category.CLIMATE, "쾌적한 온도 · 보통 풍량", false,
                    SettingKey.HVAC_POWER, true, SettingKey.CABIN_TEMPERATURE, 22.0f, SettingKey.FAN_SPEED, 3),
            PresetItem.keep(Category.WINDOW, "현재 설정 유지"),
            PresetItem.keep(Category.SEAT, "저장된 설정 유지"),
            PresetItem.keep(Category.WHEEL_HEAT, "현재 설정 유지")));

    private final String label;
    private final String description;
    public final List<PresetItem> items;

    PracticePreset(String label, String description, List<PresetItem> items) {
        this.label = label;
        this.description = description;
        this.items = Collections.unmodifiableList(items);
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String description() {
        return description;
    }
}
