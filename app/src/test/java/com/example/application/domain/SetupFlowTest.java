package com.example.application.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.application.model.DestinationGoal;
import com.example.application.model.Difficulty;
import com.example.application.model.DrivingSkill;
import com.example.application.model.Familiarity;
import com.example.application.model.GeoPoint;
import com.example.application.model.Place;
import com.example.application.model.PracticeGoalType;
import com.example.application.model.PracticeSetup;

import org.junit.Test;

public class SetupFlowTest {

    @Test
    public void onlySelectedDetailStepsAreVisited() {
        PracticeSetup s = new PracticeSetup();
        s.goalTypes.add(PracticeGoalType.SKILL);
        s.goalTypes.add(PracticeGoalType.DESTINATION);

        assertEquals(SetupStep.SKILL, SetupFlow.next(SetupStep.GOAL_TYPE, s));
        assertEquals(SetupStep.DESTINATION, SetupFlow.next(SetupStep.SKILL, s));
        assertEquals(SetupStep.DIFFICULTY, SetupFlow.next(SetupStep.DESTINATION, s));
    }

    @Test
    public void environmentOnlyGoesStraightToEnvironment() {
        PracticeSetup s = new PracticeSetup();
        s.goalTypes.add(PracticeGoalType.ENVIRONMENT);
        assertEquals(SetupStep.ENVIRONMENT, SetupFlow.next(SetupStep.GOAL_TYPE, s));
        assertEquals(SetupStep.DIFFICULTY, SetupFlow.next(SetupStep.ENVIRONMENT, s));
    }

    @Test
    public void tailOfFlowMatchesUserFlow() {
        PracticeSetup s = new PracticeSetup();
        assertEquals(SetupStep.TENSION, SetupFlow.next(SetupStep.DIFFICULTY, s));
        assertEquals(SetupStep.ROUTE, SetupFlow.next(SetupStep.TENSION, s));
        assertEquals(SetupStep.BRIEFING, SetupFlow.next(SetupStep.ROUTE, s));
        assertEquals(SetupStep.CHECKLIST, SetupFlow.next(SetupStep.BRIEFING, s));
        assertEquals(SetupStep.PRESET, SetupFlow.next(SetupStep.CHECKLIST, s));
        assertEquals(SetupStep.DEPART, SetupFlow.next(SetupStep.PRESET, s));
    }

    @Test
    public void validationRequiresInputs() {
        PracticeSetup s = new PracticeSetup();
        assertNotNull(SetupFlow.validate(SetupStep.GOAL_TYPE, s));
        s.goalTypes.add(PracticeGoalType.SKILL);
        assertNull(SetupFlow.validate(SetupStep.GOAL_TYPE, s));

        assertNotNull(SetupFlow.validate(SetupStep.SKILL, s));
        s.skills.add(DrivingSkill.U_TURN);
        assertNull(SetupFlow.validate(SetupStep.SKILL, s));

        s.difficulty = Difficulty.EASY;
        assertNotNull("지역 익숙함도 필요", SetupFlow.validate(SetupStep.DIFFICULTY, s));
        s.familiarity = Familiarity.FAMILIAR;
        assertNull(SetupFlow.validate(SetupStep.DIFFICULTY, s));

        s.destination = new DestinationGoal();
        assertNotNull(SetupFlow.validate(SetupStep.DESTINATION, s));
        s.destination.destination = new Place("마트", "", new GeoPoint(37.5, 127.04));
        assertNull(SetupFlow.validate(SetupStep.DESTINATION, s));
    }

    @Test
    public void highTensionLowersDifficultyAndRaisesGuidance() {
        PracticeSetup s = new PracticeSetup();
        s.difficulty = Difficulty.CHALLENGE;
        s.tension = 3;
        assertEquals(Difficulty.CHALLENGE, s.effectiveDifficulty());
        s.tension = 4;
        assertEquals(Difficulty.NORMAL, s.effectiveDifficulty());
        assertEquals(com.example.application.model.GuidanceLevel.ACTIVE,
                s.effectiveGuidance(com.example.application.model.GuidanceLevel.STANDARD));
    }

    @Test
    public void clearingGoalTypeDropsItsDetails() {
        PracticeSetup s = new PracticeSetup();
        s.goalTypes.add(PracticeGoalType.SKILL);
        s.skills.add(DrivingSkill.PARKING);
        s.goalTypes.remove(PracticeGoalType.SKILL);
        s.clearUnselectedDetails();
        assertEquals(0, s.skills.size());
    }

    @Test
    public void generalDriveSkipsPracticeSteps() {
        com.example.application.model.PracticeSetup s = new com.example.application.model.PracticeSetup();
        s.generalDrive = true;
        org.junit.Assert.assertEquals(SetupStep.DESTINATION, SetupFlow.first(s));
        org.junit.Assert.assertEquals(SetupStep.CHECKLIST, SetupFlow.next(SetupStep.DESTINATION, s));
        org.junit.Assert.assertEquals(SetupStep.PRESET, SetupFlow.next(SetupStep.CHECKLIST, s));
        org.junit.Assert.assertEquals(SetupStep.DEPART, SetupFlow.next(SetupStep.PRESET, s));
    }
}
