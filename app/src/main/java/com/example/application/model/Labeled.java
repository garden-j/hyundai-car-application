package com.example.application.model;

/** 선택지 화면에 표시할 이름(과 선택적 설명)을 가진 열거형 공통 인터페이스. */
public interface Labeled {
    String label();

    default String description() {
        return null;
    }
}
