package com.example.application.model;

/**
 * 앱 동작 버전.
 * 기본 버전은 Pleos SDK와 기기 내 로직만 쓰고, 상세 버전은 외부 API(카카오 길찾기·장소 검색, 기상청 날씨)를 함께 쓴다.
 */
public enum ServiceMode implements Labeled {
    BASIC("기본 버전", "Pleos SDK와 기기 안에서만 동작해요. 외부 서비스와 통신하지 않아요."),
    DETAILED("상세 버전", "카카오 지도·기상청 정보로 실제 도로와 날씨를 분석해 더 정확하게 추천해요. 인터넷 연결이 필요해요.");

    private final String label;
    private final String description;

    ServiceMode(String label, String description) {
        this.label = label;
        this.description = description;
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
