package com.example.backend.api.dto;

import com.example.backend.domain.Enums.RouteKind;
import com.example.backend.domain.GeoPoint;
import com.example.backend.domain.Place;

import java.util.List;

/** 추천 경로 후보(앱 RouteCandidate와 같은 필드 이름). briefing은 실제 회전 지점 기반 브리핑. */
public record RouteCandidateDto(
        String id,
        RouteKind kind,
        String title,
        String reason,
        int minutes,
        double distanceKm,
        int laneChanges,
        int complexIntersections,
        int merges,
        int uTurns,
        int leftTurns,
        int parkingSpots,
        int difficultyScore,
        List<String> scoreFactors,
        GeoPoint origin,
        Place destination,
        List<Place> waypoints,
        List<GeoPoint> previewPath,
        List<BriefingPointDto> briefing,
        String source) {
}
