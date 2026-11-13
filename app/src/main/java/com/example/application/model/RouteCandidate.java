package com.example.application.model;

import java.util.ArrayList;
import java.util.List;

/** 추천 경로 후보(최대 3개). 선정 이유와 난이도 점수 산출 근거를 함께 보여준다. */
public class RouteCandidate {
    public enum Kind {
        /** 넓고 단순한 길 우선. */
        WIDE_EASY,
        /** 최단 거리. */
        SHORTEST,
        /** 선택한 연습 기술을 많이 경험하도록 구성. */
        GOAL_FOCUSED
    }

    public String id;
    public Kind kind;
    public String title;
    /** 한 줄 추천 이유(예: "차선 변경이 적고 도로가 넓어요"). */
    public String reason;
    public int minutes;
    public double distanceKm;

    // 경로상 주요 요소 개수(난이도 점수 산출 근거)
    public int laneChanges;
    public int complexIntersections;
    public int merges;
    public int uTurns;
    public int leftTurns;
    public int parkingSpots;

    /** 0(쉬움) ~ 100(어려움). */
    public int difficultyScore;
    /** 점수 산출 근거 문장들. */
    public List<String> scoreFactors = new ArrayList<>();

    public GeoPoint origin;
    public Place destination;
    /** 내비게이션에 넘길 경유지(최대 2개, NaviHelper 제약). */
    public List<Place> waypoints = new ArrayList<>();
    /** 경로 미리보기용 선(서버 경로는 실제 도로 형상, 기기 내 경로는 대략적인 선). */
    public List<GeoPoint> previewPath = new ArrayList<>();
    /** 서버가 실제 회전 지점으로 만든 브리핑. 없으면 기기에서 추정해 만든다. */
    public List<BriefingPoint> briefing;
    /** 경로 출처: "KAKAO"(서버·실제 도로) 또는 null(기기 내 추정). */
    public String source;
}
