package com.example.application.data.direct.api.dto;

import com.example.application.data.direct.domain.Enums.RouteKind;
import com.example.application.data.direct.domain.GeoPoint;
import com.example.application.data.direct.domain.Place;

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
