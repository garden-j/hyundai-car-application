package com.example.application.data.remote;

import com.example.application.model.DestinationGoal;
import com.example.application.model.Difficulty;
import com.example.application.model.DrivingSkill;
import com.example.application.model.Familiarity;
import com.example.application.model.GeoPoint;
import com.example.application.model.GoalAmount;
import com.example.application.model.PracticeGoalType;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RoadEnvironment;
import com.example.application.model.TensionReason;

import java.util.Set;

/** POST /api/v1/routes/recommend 요청 본문(백엔드 RouteRecommendRequest와 같은 필드). */
class RouteRequestBody {
    GeoPoint origin;
    Set<PracticeGoalType> goalTypes;
    Set<DrivingSkill> skills;
    GoalAmount amount;
    DestinationGoal destination;
    Set<RoadEnvironment> environments;
    Difficulty difficulty;
    Familiarity familiarity;
    Integer tension;
    Set<TensionReason> tensionReasons;

    static RouteRequestBody from(GeoPoint origin, PracticeSetup s) {
        RouteRequestBody b = new RouteRequestBody();
        b.origin = origin;
        b.goalTypes = s.goalTypes;
        b.skills = s.skills;
        b.amount = s.amount;
        b.destination = s.destination;
        b.environments = s.environments;
        b.difficulty = s.difficulty;
        b.familiarity = s.familiarity;
        b.tension = s.tension;
        b.tensionReasons = s.tensionReasons;
        return b;
    }
}
