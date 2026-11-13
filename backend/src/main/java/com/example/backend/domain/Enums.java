package com.example.backend.domain;

/** 앱과 주고받는 열거형. 이름은 앱(Java enum name)과 정확히 같아야 한다. */
public final class Enums {
    private Enums() {
    }

    public enum PracticeGoalType { SKILL, DISTANCE_TIME, DESTINATION, ENVIRONMENT }

    public enum DrivingSkill { LEFT_TURN, RIGHT_TURN, U_TURN, LANE_CHANGE, INTERSECTION, MERGE, PARKING }

    public enum RoadEnvironment { RESIDENTIAL, WIDE_MULTILANE, LOW_TRAFFIC, COMPLEX_INTERSECTION, NIGHT, RAIN }

    public enum Difficulty {
        EASY, NORMAL, CHALLENGE;

        public Difficulty easier() {
            return this == CHALLENGE ? NORMAL : EASY;
        }
    }

    public enum Familiarity { FAMILIAR, NEW }

    public enum TensionReason { UNFAMILIAR_ROAD, HEAVY_TRAFFIC, WEATHER_DARK, SKILL_WORRY, PAST_EXPERIENCE, CONDITION }

    public enum RouteKind { WIDE_EASY, SHORTEST, GOAL_FOCUSED }

    public enum BriefingType { COMPLEX_INTERSECTION, MERGE, LANE_CHANGE_PREP, U_TURN, LEFT_TURN, PARKING }
}
