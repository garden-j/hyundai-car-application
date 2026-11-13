package com.example.application.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.application.R;
import com.example.application.databinding.ContentScrollBinding;
import com.example.application.model.DriverProfile;
import com.example.application.model.GuidanceLevel;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** 0-4 얼마나 자세히 안내해 드릴까요? — 안내량 3단계 + 안내 예시 미리 듣기. 언제든 설정에서 변경 가능. */
public class GuidanceLevelFragment extends BaseStepFragment {
    private TextView example;

    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("얼마나 자세히\n안내해 드릴까요?", "언제든 설정에서 바꿀 수 있어요.");
        setProgress(4, 4);
        showNav(true, false);
        DriverProfile p = services().profiles.get();
        Set<GuidanceLevel> selected = EnumSet.of(p.guidanceLevelOrDefault());
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;
        Choices.addCardGrid(list, Arrays.asList(GuidanceLevel.values()), selected, true, 3, () -> {
            if (!selected.isEmpty()) {
                showExample(selected.iterator().next(), true);
            }
        });
        list.addView(Choices.sectionTitle(requireContext(), "안내 예시 (누르면 들어요)"));
        example = new TextView(requireContext());
        example.setTextAppearance(R.style.Text_Body);
        example.setOnClickListener(v -> {
            if (!selected.isEmpty()) {
                showExample(selected.iterator().next(), true);
            }
        });
        list.addView(example);
        showExample(p.guidanceLevelOrDefault(), false);

        setPrimary("시작하기", () -> {
            p.guidanceLevel = selected.isEmpty() ? GuidanceLevel.STANDARD : selected.iterator().next();
            p.onboardingCompleted = true;
            services().profiles.save(p);
            services().tts.stop();
            nav().home();
        });
        return null;
    }

    private void showExample(GuidanceLevel level, boolean speakIt) {
        example.setText(level.example);
        if (speakIt) {
            speak(level.example.replace("\"", ""));
        }
    }
}
