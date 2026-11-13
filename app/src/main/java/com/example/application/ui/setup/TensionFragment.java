package com.example.application.ui.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.domain.SetupStep;
import com.example.application.domain.VoiceCommandParser;
import com.example.application.domain.VoiceVocabulary;
import com.example.application.model.PracticeSetup;
import com.example.application.model.TensionReason;
import com.example.application.ui.common.Choices;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 지금 얼마나 긴장되세요? — 1~5단계 + 긴장 이유(복수).
 * 4 이상이면 더 쉬운 경로를 먼저 추천하고 안내 수준을 높인다.
 */
public class TensionFragment extends BaseSetupFragment {
    private final Set<Integer> level = new LinkedHashSet<>();
    private Runnable refreshLevel;
    private Runnable refreshReasons;
    private TextView note;

    @Override
    protected SetupStep step() {
        return SetupStep.TENSION;
    }

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("지금 얼마나\n긴장되세요?", "사용자님의 상태를 알려 주세요.");
        enableVoice("3 정도야");
        setPrimary("다음", this::goNext);
        level.clear();
        if (setup().tension != null) {
            level.add(setup().tension);
        }
        LinearLayout list = scrollList(inflater);
        refreshLevel = Choices.addChipSection(list, "1 편안해요  ·  5 많이 긴장돼요",
                Arrays.asList(1, 2, 3, 4, 5), String::valueOf, level, true, this::sync);
        refreshReasons = Choices.addChipSection(list, "긴장되는 이유가 있다면 골라 주세요 (선택)",
                Arrays.asList(TensionReason.values()), setup().tensionReasons, false, null);
        note = new TextView(requireContext());
        note.setTextAppearance(R.style.Text_Body);
        note.setPadding(0, Choices.dp(requireContext(), 24), 0, 0);
        list.addView(note);
        updateNote();
        return null;
    }

    private void sync() {
        setup().tension = level.isEmpty() ? null : level.iterator().next();
        updateNote();
    }

    private void updateNote() {
        note.setText(setup().isHighTension()
                ? "괜찮아요. 더 쉬운 길을 먼저 추천하고, 안내를 조금 더 자세히 해 드릴게요."
                : "");
    }

    @Override
    protected boolean onVoiceText(String text) {
        Integer t = VoiceVocabulary.tensionLevel(text);
        Set<TensionReason> reasons = VoiceCommandParser.match(text, VoiceVocabulary.TENSION_REASONS);
        if (t == null && reasons.isEmpty()) {
            return false;
        }
        if (t != null) {
            level.clear();
            level.add(t);
            sync();
            refreshLevel.run();
        }
        setup().tensionReasons.addAll(reasons);
        refreshReasons.run();
        if (t != null) {
            speak(t + "단계로 기록했어요." + (t >= PracticeSetup.HIGH_TENSION ? " 쉬운 길부터 추천해 드릴게요." : ""));
        }
        return true;
    }
}
