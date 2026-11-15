package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.domain.VoiceVocabulary;
import com.example.application.model.DrivingSkill;
import com.example.application.ui.common.Choices;

import java.util.Arrays;
import java.util.Set;

/** ① 어떤 기술을 연습할까요? — 주행 기술 복수 선택. */
public class SkillFragment extends BaseSetupFragment {
    private Runnable refresh;

    @Override
    protected SetupStep step() {
        return SetupStep.SKILL;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("어떤 기술을\n연습할까요?", "가장 걱정되는 것부터 골라 보세요.");
        enableVoice("유턴이랑 주차");
        setPrimary("다음", this::goNext);
        refresh = Choices.addChipSection(scrollList(inflater), "연습할 기술",
                Arrays.asList(DrivingSkill.values()), setup().skills, false, null);
        return null;
    }

    @Override
    protected boolean onVoiceText(String text) {
        Set<DrivingSkill> found = VoiceCommandParser.match(text, VoiceVocabulary.SKILLS);
        if (found.isEmpty()) {
            return false;
        }
        setup().skills.addAll(found);
        refresh.run();
        speak(labels(found) + " 골랐어요.");
        return true;
    }

    static String labels(Set<? extends com.example.application.model.Labeled> set) {
        StringBuilder sb = new StringBuilder();
        for (com.example.application.model.Labeled l : set) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(l.label());
        }
        return sb.toString();
    }
}
