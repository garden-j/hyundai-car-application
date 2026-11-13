package com.example.backend.api.dto;

import com.example.backend.domain.Enums.Difficulty;
import com.example.backend.domain.Enums.DrivingSkill;
import com.example.backend.domain.Enums.Familiarity;
import com.example.backend.domain.Enums.PracticeGoalType;
import com.example.backend.domain.Enums.RoadEnvironment;
import com.example.backend.domain.Enums.TensionReason;
import com.example.backend.domain.GeoPoint;
import com.example.backend.domain.Place;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * 연습 경로 추천 요청. 앱의 PracticeSetup에서 경로 추천에 필요한 값만 보낸다.
 */
public record RouteRecommendRequest(
        @NotNull GeoPoint origin,
        Set<PracticeGoalType> goalTypes,
        Set<DrivingSkill> skills,
        @Valid GoalAmount amount,
        Destination destination,
        Set<RoadEnvironment> environments,
        Difficulty difficulty,
        Familiarity familiarity,
        @Min(1) @Max(5) Integer tension,
        Set<TensionReason> tensionReasons) {

    public static final int HIGH_TENSION = 4;

    public record GoalAmount(@NotNull Unit unit, @Min(1) int value) {
        public enum Unit { DISTANCE_KM, TIME_MIN }

        public double estimatedKm() {
            return unit == Unit.DISTANCE_KM ? value : value * 25.0 / 60.0;
        }
    }

    public record Destination(Place origin, Place destination, boolean roundTrip) {
    }

    public Set<DrivingSkill> skillsOrEmpty() {
        return skills == null ? Set.of() : skills;
    }

    public Set<RoadEnvironment> environmentsOrEmpty() {
        return environments == null ? Set.of() : environments;
    }

    public boolean hasDestination() {
        return goalTypes != null && goalTypes.contains(PracticeGoalType.DESTINATION)
                && destination != null && destination.destination() != null;
    }

    public boolean isHighTension() {
        return tension != null && tension >= HIGH_TENSION;
    }

    /** 긴장도가 높으면 한 단계 쉽게(앱과 같은 규칙). */
    public Difficulty effectiveDifficulty() {
        Difficulty base = difficulty != null ? difficulty : Difficulty.NORMAL;
        return isHighTension() ? base.easier() : base;
    }

    /** 실제 출발지: 사용자가 고른 출발지가 있으면 그것, 아니면 현재 위치. */
    public GeoPoint start() {
        if (destination != null && destination.origin() != null) {
            return destination.origin().point();
        }
        return origin;
    }
}
