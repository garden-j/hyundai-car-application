package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.application.model.DestinationGoal;
import com.example.application.model.Difficulty;
import com.example.application.model.DrivingSkill;
import com.example.application.model.GeoPoint;
import com.example.application.model.GoalAmount;
import com.example.application.model.Place;
import com.example.application.model.PracticeGoalType;
import com.example.application.model.PracticeSetup;
import com.example.application.model.RouteCandidate;

import org.junit.Test;

import java.util.List;

public class RouteCandidateGeneratorTest {
    private static final GeoPoint ORIGIN = new GeoPoint(37.4920, 127.0300);

    private static PracticeSetup skillSetup(Difficulty d) {
        PracticeSetup s = new PracticeSetup();
        s.goalTypes.add(PracticeGoalType.SKILL);
        s.skills.add(DrivingSkill.U_TURN);
        s.difficulty = d;
        return s;
    }

    @Test
    public void returnsThreeCandidatesWithWideFirstByDefault() {
        List<RouteCandidate> routes = RouteCandidateGenerator.generate(ORIGIN, skillSetup(Difficulty.NORMAL));
        assertEquals(3, routes.size());
        assertEquals(RouteCandidate.Kind.WIDE_EASY, routes.get(0).kind);
        assertEquals(RouteCandidate.Kind.SHORTEST, routes.get(1).kind);
        assertEquals(RouteCandidate.Kind.GOAL_FOCUSED, routes.get(2).kind);
        assertEquals("r1", routes.get(0).id);
    }

    @Test
    public void challengePutsGoalRouteFirstUnlessTense() {
        PracticeSetup s = skillSetup(Difficulty.CHALLENGE);
        assertEquals(RouteCandidate.Kind.GOAL_FOCUSED, RouteCandidateGenerator.generate(ORIGIN, s).get(0).kind);
        s.tension = 5;
        assertEquals(RouteCandidate.Kind.WIDE_EASY, RouteCandidateGenerator.generate(ORIGIN, s).get(0).kind);
    }

    @Test
    public void goalRouteContainsSelectedSkill() {
        RouteCandidate goal = find(RouteCandidateGenerator.generate(ORIGIN, skillSetup(Difficulty.NORMAL)),
                RouteCandidate.Kind.GOAL_FOCUSED);
        assertEquals(2, goal.uTurns);
        assertTrue(goal.reason.contains("유턴"));
    }

    @Test
    public void wideRouteIsEasierThanShortest() {
        PracticeSetup s = new PracticeSetup();
        s.goalTypes.add(PracticeGoalType.DISTANCE_TIME);
        s.amount = new GoalAmount(GoalAmount.Unit.DISTANCE_KM, 10);
        List<RouteCandidate> routes = RouteCandidateGenerator.generate(ORIGIN, s);
        RouteCandidate wide = find(routes, RouteCandidate.Kind.WIDE_EASY);
        RouteCandidate shortest = find(routes, RouteCandidate.Kind.SHORTEST);
        assertTrue(wide.difficultyScore < shortest.difficultyScore);
        assertTrue(wide.distanceKm > shortest.distanceKm);
        assertFalse(wide.scoreFactors.isEmpty());
    }

    @Test
    public void courseWithoutDestinationLoopsBackToOrigin() {
        RouteCandidate r = RouteCandidateGenerator.generate(ORIGIN, skillSetup(Difficulty.EASY)).get(0);
        assertEquals(2, r.waypoints.size());
        assertEquals(ORIGIN.latitude, r.destination.point.latitude, 1e-9);
    }

    @Test
    public void roundTripUsesDestinationAsWaypoint() {
        PracticeSetup s = new PracticeSetup();
        s.goalTypes.add(PracticeGoalType.DESTINATION);
        s.destination = new DestinationGoal();
        s.destination.destination = new Place("마트", "", new GeoPoint(37.4993, 127.0461));
        s.destination.roundTrip = true;
        RouteCandidate r = find(RouteCandidateGenerator.generate(ORIGIN, s), RouteCandidate.Kind.SHORTEST);
        assertEquals("마트", r.waypoints.get(0).name);
        assertEquals(ORIGIN.latitude, r.destination.point.latitude, 1e-9);
        double oneWay = ORIGIN.distanceKmTo(s.destination.destination.point) * RouteCandidateGenerator.ROAD_FACTOR;
        assertEquals(Math.round(oneWay * 2 * 10) / 10.0, r.distanceKm, 0.11);
    }

    @Test
    public void sameInputSameOutput() {
        List<RouteCandidate> a = RouteCandidateGenerator.generate(ORIGIN, skillSetup(Difficulty.NORMAL));
        List<RouteCandidate> b = RouteCandidateGenerator.generate(ORIGIN, skillSetup(Difficulty.NORMAL));
        for (int i = 0; i < 3; i++) {
            assertEquals(a.get(i).difficultyScore, b.get(i).difficultyScore);
            assertEquals(a.get(i).minutes, b.get(i).minutes);
        }
    }

    private static RouteCandidate find(List<RouteCandidate> list, RouteCandidate.Kind kind) {
        for (RouteCandidate r : list) {
            if (r.kind == kind) {
                return r;
            }
        }
        throw new AssertionError("missing " + kind);
    }
}
