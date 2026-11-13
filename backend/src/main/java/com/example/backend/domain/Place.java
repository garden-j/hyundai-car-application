package com.example.backend.domain;

/** 이름이 있는 장소. 앱의 Place와 같은 필드 이름. */
public record Place(String name, String address, GeoPoint point) {
}
