package com.example.application.core.pleos.vehicle;

import com.example.application.core.Callback;
import com.example.application.core.MainThread;

/**
 * Pleos 서비스가 없는 환경(일반 에뮬레이터, 단위 테스트, PT 데모)용 가상 차량.
 * 실제 차량처럼 비동기로 응답하고, 김서림 제거는 "미지원"으로 두어 적용 실패 흐름도 확인할 수 있게 한다.
 */
public class FakeVehicleGateway implements VehicleGateway {
    private static final long LATENCY_MS = 150;

    private final VehicleSettings state = new VehicleSettings();
    private final VehicleReadiness readiness = new VehicleReadiness();
    private boolean defrosterSupported = false;

    public FakeVehicleGateway() {
        state.hvacPower = true;
        state.cabinTemperature = 25.5f;
        state.fanSpeed = 5;
        state.driverWindow = 0;
        state.steeringWheelHeat = 0;
        state.defroster = null; // 미지원

        readiness.driverSeatBeltBuckled = false;
        readiness.allDoorsClosed = true;
        readiness.fuelPercent = 62;
        readiness.evBatteryLevel = null;
        readiness.rangeKm = 300f;
    }

    /** 데모 시나리오에서 상태를 바꿀 때 사용. */
    public void setSeatBeltBuckled(boolean buckled) {
        readiness.driverSeatBeltBuckled = buckled;
    }

    public void setDefrosterSupported(boolean supported) {
        defrosterSupported = supported;
        state.defroster = supported ? Boolean.FALSE : null;
    }

    @Override
    public void connect() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public void readSettings(Callback<VehicleSettings> callback) {
        VehicleSettings copy = state.copy();
        MainThread.postDelayed(() -> callback.onSuccess(copy), LATENCY_MS);
    }

    @Override
    public void applySetting(SettingKey key, Object value, Callback<Object> callback) {
        MainThread.postDelayed(() -> {
            if (key == SettingKey.DEFROSTER && !defrosterSupported) {
                callback.onError(new UnsupportedOperationException("이 차량은 김서림 제거 제어를 지원하지 않아요"));
                return;
            }
            state.set(key, value);
            callback.onSuccess(value);
        }, LATENCY_MS);
    }

    @Override
    public void readFanSpeedRange(Callback<int[]> callback) {
        MainThread.postDelayed(() -> callback.onSuccess(new int[]{1, 8}), LATENCY_MS);
    }

    @Override
    public void readReadiness(Callback<VehicleReadiness> callback) {
        VehicleReadiness copy = new VehicleReadiness();
        copy.driverSeatBeltBuckled = readiness.driverSeatBeltBuckled;
        copy.allDoorsClosed = readiness.allDoorsClosed;
        copy.fuelPercent = readiness.fuelPercent;
        copy.evBatteryLevel = readiness.evBatteryLevel;
        copy.rangeKm = readiness.rangeKm;
        MainThread.postDelayed(() -> callback.onSuccess(copy), LATENCY_MS);
        // 데모: 첫 확인 이후 운전자가 안전벨트를 맨 것으로 가정
        readiness.driverSeatBeltBuckled = true;
    }

    @Override
    public void readStatus(Callback<VehicleStatus> callback) {
        VehicleStatus st = new VehicleStatus();
        st.gear = VehicleStatus.Gear.PARK;
        st.wiperOn = false;
        st.lowBeamOn = false;
        st.allDoorsClosed = true;
        st.engineOil = "NORMAL";
        st.brakeFluid = "NORMAL";
        MainThread.post(() -> callback.onSuccess(st));
    }
}
