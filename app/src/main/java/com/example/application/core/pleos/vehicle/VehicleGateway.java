package com.example.application.core.pleos.vehicle;

import com.example.application.core.Callback;

/**
 * Vehicle SDK 래퍼. 화면·도메인 코드는 SDK 대신 이 인터페이스만 사용한다.
 * 실제 구현({@link PleosVehicleGateway})과 Pleos 서비스가 없는 환경용 {@link FakeVehicleGateway}가 있다.
 */
public interface VehicleGateway {
    void connect();

    void release();

    /** Pleos 서비스 없이 시뮬레이션 중이면 true. */
    boolean isSimulated();

    /** 제어 대상 설정 전체를 읽는다. 항목별 실패는 null로 채워 성공으로 전달한다. */
    void readSettings(Callback<VehicleSettings> callback);

    /**
     * 설정 하나를 바꾼다. 성공 시 차량이 응답한 값(없으면 요청 값)을 돌려준다.
     *
     * @param value {@link SettingKey}에 맞는 타입(Float/Integer/Boolean)
     */
    void applySetting(SettingKey key, Object value, Callback<Object> callback);

    /** 운전석 풍량 범위 [min, max]. 실패 시 onError. */
    void readFanSpeedRange(Callback<int[]> callback);

    /** 출발 전 체크리스트용 차량 상태. */
    void readReadiness(Callback<VehicleReadiness> callback);
}
