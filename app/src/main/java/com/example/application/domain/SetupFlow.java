package com.example.application.domain;

import com.example.application.model.PracticeGoalType;
import com.example.application.model.PracticeSetup;

/**
 * 출발 전 설정 흐름의 순서와 단계별 입력 검증.
 * 목표 유형 → (선택한 유형의 세부 설정) → 난이도·지역 익숙함 → 긴장도 → 경로 추천
 * → 복잡도 브리핑 → 준비 체크리스트 → 연습 모드 프리셋 → 출발.
 */
public final class SetupFlow {
    private SetupFlow() {
    }

    public static SetupStep first() {
        return SetupStep.GOAL_TYPE;
    }

    public static SetupStep next(SetupStep current, PracticeSetup s) {
        switch (current) {
            case GOAL_TYPE:
                return firstDetailAfter(-1, s);
            case SKILL:
                return firstDetailAfter(0, s);
            case DISTANCE_TIME:
                return firstDetailAfter(1, s);
            case DESTINATION:
                return firstDetailAfter(2, s);
            case ENVIRONMENT:
                return SetupStep.DIFFICULTY;
            case DIFFICULTY:
                return SetupStep.TENSION;
            case TENSION:
                return SetupStep.ROUTE;
            case ROUTE:
                return SetupStep.BRIEFING;
            case BRIEFING:
                return SetupStep.CHECKLIST;
            case CHECKLIST:
                return SetupStep.PRESET;
            case PRESET:
                return SetupStep.DEPART;
            default:
                throw new IllegalStateException("no step after " + current);
        }
    }

    private static final PracticeGoalType[] DETAIL_ORDER = {
            PracticeGoalType.SKILL, PracticeGoalType.DISTANCE_TIME,
            PracticeGoalType.DESTINATION, PracticeGoalType.ENVIRONMENT
    };
    private static final SetupStep[] DETAIL_STEPS = {
            SetupStep.SKILL, SetupStep.DISTANCE_TIME, SetupStep.DESTINATION, SetupStep.ENVIRONMENT
    };

    private static SetupStep firstDetailAfter(int index, PracticeSetup s) {
        for (int i = index + 1; i < DETAIL_ORDER.length; i++) {
            if (s.goalTypes.contains(DETAIL_ORDER[i])) {
                return DETAIL_STEPS[i];
            }
        }
        return SetupStep.DIFFICULTY;
    }

    /** 다음으로 넘어갈 수 없으면 사용자에게 보여줄 안내 문구, 가능하면 null. */
    public static String validate(SetupStep step, PracticeSetup s) {
        switch (step) {
            case GOAL_TYPE:
                return s.goalTypes.isEmpty() ? "연습 유형을 하나 이상 골라 주세요." : null;
            case SKILL:
                return s.skills.isEmpty() ? "연습할 기술을 하나 이상 골라 주세요." : null;
            case DISTANCE_TIME:
                return s.amount == null ? "거리나 시간을 정해 주세요." : null;
            case DESTINATION:
                return s.destination == null || s.destination.destination == null
                        ? "목적지를 골라 주세요." : null;
            case ENVIRONMENT:
                return s.environments.isEmpty() ? "익숙해지고 싶은 환경을 하나 이상 골라 주세요." : null;
            case DIFFICULTY:
                if (s.difficulty == null) {
                    return "연습 난이도를 골라 주세요.";
                }
                return s.familiarity == null ? "익숙한 곳과 새로운 곳 중에 골라 주세요." : null;
            case TENSION:
                return s.tension == null ? "지금 긴장되는 정도를 1에서 5 사이로 골라 주세요." : null;
            case ROUTE:
                return s.selectedRoute == null ? "연습할 경로를 골라 주세요." : null;
            case PRESET:
                return s.preset == null ? "연습 모드를 불러오는 중이에요." : null;
            default:
                return null;
        }
    }
}
