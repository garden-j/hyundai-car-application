package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.domain.VoiceVocabulary;
import com.example.application.model.RoadEnvironment;
import com.example.application.ui.common.Choices;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** ④ 어떤 길에 익숙해질까요? — 도로 유형·교통 환경·시간/날씨 그룹별 복수 선택. */
public class EnvironmentFragment extends BaseSetupFragment {
    private final List<Runnable> refreshers = new ArrayList<>();

    @Override
    protected SetupStep step() {
        return SetupStep.ENVIRONMENT;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("어떤 길에\n익숙해질까요?", "도로, 교통, 시간대에서 골라 주세요.");
        enableVoice("좁은 길이랑 야간");
        setPrimary("다음", this::goNext);
        LinearLayout list = scrollList(inflater);
        refreshers.clear();
        for (RoadEnvironment.Group g : RoadEnvironment.Group.values()) {
            List<RoadEnvironment> values = new ArrayList<>();
            for (RoadEnvironment e : RoadEnvironment.values()) {
                if (e.group == g) {
                    values.add(e);
                }
            }
            refreshers.add(Choices.addChipSection(list, g.label, values, setup().environments, false, null));
        }
        return null;
    }

    @Override
    protected boolean onVoiceText(String text) {
        Set<RoadEnvironment> found = VoiceCommandParser.match(text, VoiceVocabulary.ENVIRONMENTS);
        if (found.isEmpty()) {
            return false;
        }
        setup().environments.addAll(found);
        for (Runnable r : refreshers) {
            r.run();
        }
        speak(SkillFragment.labels(found) + " 골랐어요.");
        return true;
    }
}
