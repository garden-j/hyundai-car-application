package com.example.application.model;

import com.example.application.core.pleos.vehicle.SettingKey;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 출발 전 설정 흐름에서 모으는 연습 설정 초안.
 * 최종 결과(경로·난이도·긴장도)는 주행 중 안내 수준과 Relax 안내 설정에 반영된다.
 */
public class PracticeSetup {
    public static final int HIGH_TENSION = 4;

    public Set<PracticeGoalType> goalTypes = EnumSet.noneOf(PracticeGoalType.class);
    public Set<DrivingSkill> skills = EnumSet.noneOf(DrivingSkill.class);
    public GoalAmount amount;
    public DestinationGoal destination;
    public Set<RoadEnvironment> environments = EnumSet.noneOf(RoadEnvironment.class);
    public Difficulty difficulty;
    public Familiarity familiarity;
    /** 1(편안해요) ~ 5(많이 긴장돼요). */
    public Integer tension;
    public Set<TensionReason> tensionReasons = EnumSet.noneOf(TensionReason.class);

    /** 위치를 알 수 없을 때 사용자가 직접 고른 출발지. */
    public Place manualOrigin;

    public RouteCandidate selectedRoute;
    public PracticePreset preset;
    /** "항목 수정"에서 사용자가 바꾼 값. null 값은 "그대로 두기". */
    public Map<SettingKey, Object> presetOverrides = new EnumMap<>(SettingKey.class);
    /** 동의가 필요한 프리셋 항목(핸들 열선 등)에 동의했는지. */
    public boolean presetConsentGiven;

    public boolean hasDestination() {
        return goalTypes.contains(PracticeGoalType.DESTINATION)
                && destination != null && destination.destination != null;
    }

    public boolean isHighTension() {
        return tension != null && tension >= HIGH_TENSION;
    }

    /** 긴장도가 높으면 한 단계 쉬운 난이도로 경로를 추천한다(유저플로우: "더 쉬운 경로를 우선 추천"). */
    public Difficulty effectiveDifficulty() {
        Difficulty base = difficulty != null ? difficulty : Difficulty.NORMAL;
        return isHighTension() ? base.easier() : base;
    }

    /** 긴장도가 높으면 주행 중 안내 수준을 한 단계 올린다. */
    public GuidanceLevel effectiveGuidance(GuidanceLevel profileLevel) {
        GuidanceLevel base = profileLevel != null ? profileLevel : GuidanceLevel.STANDARD;
        return isHighTension() ? base.raised() : base;
    }

    /** 선택한 유형에 해당하지 않는 세부 설정은 지운다(유형 선택을 바꿨을 때). */
    public void clearUnselectedDetails() {
        if (!goalTypes.contains(PracticeGoalType.SKILL)) {
            skills.clear();
        }
        if (!goalTypes.contains(PracticeGoalType.DISTANCE_TIME)) {
            amount = null;
        }
        if (!goalTypes.contains(PracticeGoalType.DESTINATION)) {
            destination = null;
        }
        if (!goalTypes.contains(PracticeGoalType.ENVIRONMENT)) {
            environments.clear();
        }
    }
}
