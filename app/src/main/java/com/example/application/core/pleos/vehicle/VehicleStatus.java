package com.example.application.core.pleos.vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * 주행 중·차량 도움 화면용 차량 상태. 읽지 못한 항목은 null.
 */
public class VehicleStatus {
    public enum Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

    public Gear gear;
    /** 앞유리 와이퍼 작동 중(비 추정). */
    public Boolean wiperOn;
    /** 전조등(하향등) 켜짐(야간·터널 추정). */
    public Boolean lowBeamOn;
    public Boolean allDoorsClosed;
    /** 엔진오일·브레이크액 상태: NORMAL, LOW, CRITICALLY_LOW, HIGH, ERROR. */
    public String engineOil;
    public String brakeFluid;
    /** 켜져 있거나 깜빡이는 경고등 종류(TelltaleType 이름). */
    public List<String> warnings = new ArrayList<>();

    public boolean isParked() {
        return gear == Gear.PARK;
    }
}
