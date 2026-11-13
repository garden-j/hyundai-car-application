package com.example.application.ui.setup;

import androidx.fragment.app.Fragment;

import com.example.application.domain.SetupStep;
import com.example.application.ui.depart.DepartFragment;

/** 설정 단계 → 화면 매핑. */
public final class SetupScreens {
    private SetupScreens() {
    }

    public static Fragment create(SetupStep step) {
        switch (step) {
            case GOAL_TYPE:
                return new GoalTypeFragment();
            case SKILL:
                return new SkillFragment();
            case DISTANCE_TIME:
                return new DistanceTimeFragment();
            case DESTINATION:
                return new DestinationFragment();
            case ENVIRONMENT:
                return new EnvironmentFragment();
            case DIFFICULTY:
                return new DifficultyFragment();
            case TENSION:
                return new TensionFragment();
            case ROUTE:
                return new RouteFragment();
            case BRIEFING:
                return new BriefingFragment();
            case CHECKLIST:
                return new ChecklistFragment();
            case PRESET:
                return new PresetFragment();
            case DEPART:
            default:
                return new DepartFragment();
        }
    }
}
