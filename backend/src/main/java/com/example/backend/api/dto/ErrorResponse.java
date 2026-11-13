package com.example.backend.api.dto;

/** 오류 응답. code는 앱이 분기할 때 쓰는 고정 문자열. */
public record ErrorResponse(String code, String message) {
}
