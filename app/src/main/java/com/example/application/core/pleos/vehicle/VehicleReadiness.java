package com.example.application.core.pleos.vehicle;

/**
 * 출발 전 체크리스트에서 차량 데이터로 자동 확인하는 항목.
 * null은 "차량에서 확인할 수 없음" → 사용자가 직접 확인하도록 안내한다.
 */
public class VehicleReadiness {
    public Boolean driverSeatBeltBuckled;
    public Boolean allDoorsClosed;
    /** 연료 잔량(%). 전기차이거나 읽기 실패 시 null. */
    public Integer fuelPercent;
    /** 전기차 배터리 잔량(%). 게이트웨이가 용량 대비 비율로 바꿔 채운다. 내연기관이거나 읽기 실패 시 null. */
    public Float evBatteryLevel;
    /** 주행 가능 거리(km). 읽기 실패 시 null. */
    public Float rangeKm;
}
