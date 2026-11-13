package com.example.application.core.pleos.vehicle;

/**
 * 앱이 읽고 바꾸는 차량 설정 항목. 값 타입은 항목마다 다르다.
 * 음악 음량은 Pleos SDK에 제어 API가 없어 여기에 포함하지 않는다(사용자 안내로 대체).
 * 좌석은 "저장된 운전자 설정 유지"가 정책이라 제어 대상이 아니다.
 */
public enum SettingKey {
    /** 공조 전원(Boolean). 꺼져 있으면 온도를 읽거나 바꿀 수 없어 다른 공조 항목보다 먼저 적용한다. */
    HVAC_POWER("공조 전원"),
    /** 운전석 공조 온도(섭씨, Float). */
    CABIN_TEMPERATURE("공조 온도"),
    /** 운전석 풍량(Integer, 차량별 범위). */
    FAN_SPEED("풍량"),
    /** 운전석 창문 위치(Integer, 0 = 닫힘). */
    DRIVER_WINDOW("운전석 창문"),
    /** 핸들 열선 단계(Integer, 0 = 꺼짐). */
    STEERING_WHEEL_HEAT("핸들 열선"),
    /** 앞유리 김서림 제거(Boolean). */
    DEFROSTER("김서림 제거");

    public final String label;

    SettingKey(String label) {
        this.label = label;
    }
}
