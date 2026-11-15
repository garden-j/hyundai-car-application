package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.domain.VoiceVocabulary;
import com.example.application.model.Difficulty;
import com.example.application.model.Familiarity;
import com.example.application.ui.common.Choices;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** 연습 난이도(쉬움·보통·도전) + 연습 조건(지역 익숙함). */
public class DifficultyFragment extends BaseSetupFragment {
    private final Set<Difficulty> difficulty = EnumSet.noneOf(Difficulty.class);
    private final Set<Familiarity> familiarity = EnumSet.noneOf(Familiarity.class);
    private Runnable refreshDifficulty;
    private Runnable refreshFamiliarity;

    @Override
    protected SetupStep step() {
        return SetupStep.DIFFICULTY;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("얼마나 어렵게 해 볼까요?", "고른 대로 길의 복잡도를 맞춰요.");
        enableVoice("쉬움, 익숙한 곳");
        setPrimary("다음", this::goNext);
        difficulty.clear();
        familiarity.clear();
        if (setup().difficulty != null) {
            difficulty.add(setup().difficulty);
        }
        if (setup().familiarity != null) {
            familiarity.add(setup().familiarity);
        }
        LinearLayout list = scrollList(inflater);
        refreshDifficulty = Choices.addChipSection(list, "연습 난이도", Arrays.asList(Difficulty.values()),
                difficulty, true, this::sync);
        refreshFamiliarity = Choices.addChipSection(list, "이 지역은 어떤가요?", Arrays.asList(Familiarity.values()),
                familiarity, true, this::sync);
        return null;
    }

    private void sync() {
        setup().difficulty = difficulty.isEmpty() ? null : difficulty.iterator().next();
        setup().familiarity = familiarity.isEmpty() ? null : familiarity.iterator().next();
    }

    @Override
    protected boolean onVoiceText(String text) {
        Difficulty d = VoiceCommandParser.matchFirst(text, VoiceVocabulary.DIFFICULTIES);
        Familiarity f = VoiceCommandParser.matchFirst(text, VoiceVocabulary.FAMILIARITY);
        if (d == null && f == null) {
            return false;
        }
        if (d != null) {
            difficulty.clear();
            difficulty.add(d);
        }
        if (f != null) {
            familiarity.clear();
            familiarity.add(f);
        }
        sync();
        refreshDifficulty.run();
        refreshFamiliarity.run();
        if (setup().difficulty != null && setup().familiarity != null) {
            goNext();
        }
        return true;
    }
}
