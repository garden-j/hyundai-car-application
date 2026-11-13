package com.example.application.domain;

/** 출발 전 설정 흐름의 화면 단계(유저플로우 FigJam "연습 시작" ~ "출발"). */
public enum SetupStep {
    GOAL_TYPE,
    SKILL,
    DISTANCE_TIME,
    DESTINATION,
    ENVIRONMENT,
    DIFFICULTY,
    TENSION,
    ROUTE,
    BRIEFING,
    CHECKLIST,
    PRESET,
    /** 출발 → 주행 중 화면(3단계에서 구현). */
    DEPART
}
