package com.example.backend.api.dto;

import com.example.backend.domain.Enums.BriefingType;
import com.example.backend.domain.GeoPoint;

/** 복잡 구간 브리핑 한 곳(앱 BriefingPoint와 같은 필드). */
public record BriefingPointDto(int order, BriefingType type, double distanceKm, String instruction,
                               String speech, GeoPoint point) {
}
