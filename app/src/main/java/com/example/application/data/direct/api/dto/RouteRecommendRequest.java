package com.example.application.data.direct.api.dto;

import com.example.application.data.direct.common.Lists;

import com.example.application.data.direct.domain.Enums.Difficulty;
import com.example.application.data.direct.domain.Enums.DrivingSkill;
import com.example.application.data.direct.domain.Enums.Familiarity;
import com.example.application.data.direct.domain.Enums.PracticeGoalType;
import com.example.application.data.direct.domain.Enums.RoadEnvironment;
import com.example.application.data.direct.domain.Enums.TensionReason;
import com.example.application.data.direct.domain.GeoPoint;
import com.example.application.data.direct.domain.Place;


import java.util.Set;

/**
 * 연습 경로 추천 요청. 앱의 PracticeSetup에서 경로 추천에 필요한 값만 보낸다.
 */
public record RouteRecommendRequest(
        GeoPoint origin,
        Set<PracticeGoalType> goalTypes,
        Set<DrivingSkill> skills,
        GoalAmount amount,
        Destination destination,
        Set<RoadEnvironment> environments,
        Difficulty difficulty,
        Familiarity familiarity,
        Integer tension,
        Set<TensionReason> tensionReasons) {

    public static final int HIGH_TENSION = 4;

    public record GoalAmount(Unit unit, int value) {
        public enum Unit { DISTANCE_KM, TIME_MIN }

        public double estimatedKm() {
            return unit == Unit.DISTANCE_KM ? value : value * 25.0 / 60.0;
        }
    }

    public record Destination(Place origin, Place destination, boolean roundTrip) {
    }

    public Set<DrivingSkill> skillsOrEmpty() {
        return skills == null ? Lists.setOf() : skills;
    }

    public Set<RoadEnvironment> environmentsOrEmpty() {
        return environments == null ? Lists.setOf() : environments;
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
