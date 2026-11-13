package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.domain.VoiceVocabulary;
import com.example.application.model.PracticeGoalType;
import com.example.application.ui.common.Choices;

import java.util.Arrays;
import java.util.Set;

/** 오늘은 어떤 연습을 할까요? — 목표 유형 4종 복수 선택. 선택한 유형만 세부 설정한다. */
public class GoalTypeFragment extends BaseSetupFragment {
    private Runnable refresh;

    @Override
    protected SetupStep step() {
        return SetupStep.GOAL_TYPE;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("오늘은 어떤\n연습을 할까요?", "여러 개 골라도 괜찮아요.");
        enableVoice("주행 기술과 목적지");
        setPrimary("다음", this::goNext);
        refresh = Choices.addCardGrid(scrollList(inflater), Arrays.asList(PracticeGoalType.values()),
                setup().goalTypes, false, 2, () -> setup().clearUnselectedDetails());
        return null;
    }

    @Override
    protected boolean onVoiceText(String text) {
        Set<PracticeGoalType> found = VoiceCommandParser.match(text, VoiceVocabulary.GOAL_TYPES);
        if (found.isEmpty()) {
            return false;
        }
        setup().goalTypes.clear();
        setup().goalTypes.addAll(found);
        setup().clearUnselectedDetails();
        refresh.run();
        goNext();
        return true;
    }
}
