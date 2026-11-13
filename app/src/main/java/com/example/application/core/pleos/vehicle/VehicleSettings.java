package com.example.application.core.pleos.vehicle;

import java.util.Objects;

/**
 * 차량 설정값 스냅샷. 값이 null이면 읽기에 실패했거나 차량이 지원하지 않는 항목이다.
 * 연습 시작 전 값을 저장해 두었다가 연습 종료 후 복원에 사용한다(JSON 직렬화 대상).
 */
public class VehicleSettings {
    public Boolean hvacPower;
    public Float cabinTemperature;
    public Integer fanSpeed;
    public Integer driverWindow;
    public Integer steeringWheelHeat;
    public Boolean defroster;

    public Object get(SettingKey key) {
        switch (key) {
            case HVAC_POWER:
                return hvacPower;
            case CABIN_TEMPERATURE:
                return cabinTemperature;
            case FAN_SPEED:
                return fanSpeed;
            case DRIVER_WINDOW:
                return driverWindow;
            case STEERING_WHEEL_HEAT:
                return steeringWheelHeat;
            case DEFROSTER:
                return defroster;
            default:
                throw new IllegalArgumentException(key.name());
        }
    }

    public void set(SettingKey key, Object value) {
        switch (key) {
            case HVAC_POWER:
                hvacPower = (Boolean) value;
                break;
            case CABIN_TEMPERATURE:
                cabinTemperature = value == null ? null : ((Number) value).floatValue();
                break;
            case FAN_SPEED:
                fanSpeed = value == null ? null : ((Number) value).intValue();
                break;
            case DRIVER_WINDOW:
                driverWindow = value == null ? null : ((Number) value).intValue();
                break;
            case STEERING_WHEEL_HEAT:
                steeringWheelHeat = value == null ? null : ((Number) value).intValue();
                break;
            case DEFROSTER:
                defroster = (Boolean) value;
                break;
            default:
                throw new IllegalArgumentException(key.name());
        }
    }

    public VehicleSettings copy() {
        VehicleSettings c = new VehicleSettings();
        for (SettingKey k : SettingKey.values()) {
            c.set(k, get(k));
        }
        return c;
    }

    /** 화면 표시용 값 문자열. */
    public static String format(SettingKey key, Object value) {
        if (value == null) {
            return "확인 불가";
        }
        switch (key) {
            case HVAC_POWER:
                return ((Boolean) value) ? "켜짐" : "꺼짐";
            case CABIN_TEMPERATURE:
                return String.format(java.util.Locale.KOREA, "%.1f℃", ((Number) value).floatValue());
            case FAN_SPEED:
                return ((Number) value).intValue() + "단";
            case DRIVER_WINDOW:
                return ((Number) value).intValue() == 0 ? "닫힘" : "열림(" + value + ")";
            case STEERING_WHEEL_HEAT:
                return ((Number) value).intValue() == 0 ? "꺼짐" : ((Number) value).intValue() + "단";
            case DEFROSTER:
                return ((Boolean) value) ? "켜짐" : "꺼짐";
            default:
                return String.valueOf(value);
        }
    }

    public static boolean sameValue(SettingKey key, Object a, Object b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }
        if (key == SettingKey.CABIN_TEMPERATURE) {
            return Math.abs(((Number) a).floatValue() - ((Number) b).floatValue()) < 0.05f;
        }
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).intValue() == ((Number) b).intValue();
        }
        return a.equals(b);
    }
}
