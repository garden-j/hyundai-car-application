package com.example.application.model;

/** 목적별로 따로 동의받는 권한(온보딩 0-1). */
public enum PermissionType implements Labeled {
    LOCATION("위치 정보",
            "연습 경로 추천, 주변 주유소·휴식 장소 찾기에 사용해요.",
            "현재 위치 대신 출발지를 직접 골라야 하고, 주변 시설 안내를 받을 수 없어요."),
    MICROPHONE("마이크",
            "말로 답하기, 음성 질문(\"와이퍼 어떻게 켜?\")에 사용해요.",
            "음성으로 답할 수 없고 화면을 눌러서 선택해야 해요."),
    VEHICLE("차량 정보·제어",
            "안전벨트·연료 확인, 연습 모드 공조·창문 설정에 사용해요.",
            "차량 상태 자동 확인과 연습 모드 차량 환경 조절을 쓸 수 없어요.");

    private final String label;
    private final String description;
    public final String limitationWhenDenied;

    PermissionType(String label, String description, String limitationWhenDenied) {
        this.label = label;
        this.description = description;
        this.limitationWhenDenied = limitationWhenDenied;
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
